package com.joejoe2.chat.service.channel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.joejoe2.chat.data.PageRequest;
import com.joejoe2.chat.data.message.PrivateMessageDto;
import com.joejoe2.chat.data.SliceList;
import com.joejoe2.chat.data.channel.profile.PrivateChannelProfile;
import com.joejoe2.chat.exception.AlreadyExist;
import com.joejoe2.chat.exception.ChannelDoesNotExist;
import com.joejoe2.chat.exception.InvalidOperation;
import com.joejoe2.chat.exception.UserDoesNotExist;
import com.joejoe2.chat.models.PrivateChannel;
import com.joejoe2.chat.models.User;
import com.joejoe2.chat.repository.channel.PrivateChannelRepository;
import com.joejoe2.chat.repository.user.UserRepository;
import com.joejoe2.chat.utils.SseUtil;
import com.joejoe2.chat.utils.SubjectPrefix;
import com.joejoe2.chat.validation.validator.PageRequestValidator;
import com.joejoe2.chat.validation.validator.UUIDValidator;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class PrivateChannelServiceImpl implements PrivateChannelService {
    @Autowired
    UserRepository userRepository;
    @Autowired
    PrivateChannelRepository channelRepository;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    UUIDValidator uuidValidator;
    @Autowired
    PageRequestValidator pageValidator;

    Map<String, Set<SseEmitter>> listeningUsers = new ConcurrentHashMap<>();
    private static final int MAX_CONNECT_DURATION = 15;
    private ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(1);

    private final String SUBJECT_PREFIX = SubjectPrefix.PRIVATE_CHANNEL;
    private static final Logger logger = LoggerFactory.getLogger(PrivateChannelService.class);

    @Autowired
    Connection connection;

    Dispatcher dispatcher;

    /**
     * create nats dispatcher with shared message handler for all
     * private messages after bean is constructed, the shared message
     * handler will deliver private messages to registered users(subscribers)
     * on this server
     */
    @PostConstruct
    private void initNats() {
        dispatcher = connection.createDispatcher((msg) -> {
            try {
                sendToSubscribers(listeningUsers.get(msg.getSubject().replace(SUBJECT_PREFIX, "")),
                        objectMapper.readValue(new String(msg.getData(), StandardCharsets.UTF_8),
                                PrivateMessageDto.class));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * deliver private messages to registered users(subscribers)
     */
    private void sendToSubscribers(Set<SseEmitter> subscribers, PrivateMessageDto message) {
        new ArrayList<>(subscribers).parallelStream().forEach((subscriber) -> {
            try {
                SseUtil.sendMessageEvent(subscriber, message);
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        });
    }

    @Override
    public SseEmitter subscribe(String fromUserId) throws UserDoesNotExist {
        userRepository.findById(uuidValidator.validate(fromUserId))
                .orElseThrow(() -> new UserDoesNotExist("user is not exist !"));

        SseEmitter subscriber = createUserSubscriber(fromUserId);
        SseUtil.sendConnectEvent(subscriber);
        return subscriber;
    }

    /**
     * create SseEmitter instance(subscriber)
     * @param userId
     * @return
     */
    private SseEmitter createUserSubscriber(String userId) {
        SseEmitter subscriber = new SseEmitter(120000L);
        addUnSubscribeTriggers(userId, subscriber);
        listenToUser(subscriber, userId);
        return subscriber;
    }

    /**
     * add UnSubscribe listener to SseEmitter instance(subscriber), and force
     * unsubscribing after MAX_CONNECT_DURATION MINUTES
     * @param userId
     * @param subscriber
     */
    private void addUnSubscribeTriggers(String userId, SseEmitter subscriber){
        Runnable unSubscribe =  () ->listeningUsers.compute(userId, (key, val) -> {
            //remove from subscribers
            if (val != null) val.remove(subscriber);
            //unsubscribe if no subscribers
            if (val == null || val.isEmpty()) {
                dispatcher.unsubscribe(SUBJECT_PREFIX + userId);
                return null;
            }
            return val;
        });
        SseUtil.addSseCallbacks(subscriber, unSubscribe);
        scheduler.schedule(unSubscribe, MAX_CONNECT_DURATION, TimeUnit.MINUTES);
    }

    /**
     * register SseEmitter instance(subscriber) and channelId to nats dispatcher
     * @param subscriber
     * @param userId
     */
    private void listenToUser(SseEmitter subscriber, String userId) {
        listeningUsers.compute(userId, (key, subscribers) -> {
            if (subscribers == null) {
                subscribers = Collections.synchronizedSet(new HashSet<>());
            }
            subscribers.add(subscriber);
            dispatcher.subscribe(SUBJECT_PREFIX + userId);
            return subscribers;
        });
    }

    @Override
    public PrivateChannelProfile createChannelBetween(String fromUserId, String toUserId) throws UserDoesNotExist, AlreadyExist, InvalidOperation {
        User user = userRepository.findById(uuidValidator.validate(fromUserId))
                .orElseThrow(() -> new UserDoesNotExist("user is not exist !"));
        User targetUser = userRepository.findById(uuidValidator.validate(toUserId))
                .orElseThrow(() -> new UserDoesNotExist("target user is not exist !"));
        if (user.equals(targetUser))
            throw new InvalidOperation("cannot chat with yourself !");
        if (channelRepository.isPrivateChannelExistBetween(user, targetUser))
            throw new AlreadyExist("channel between " + user.getUserName() + " and " + targetUser.getUserName() + " is already exist !");

        PrivateChannel channel = new PrivateChannel(new HashSet<>(Arrays.asList(user, targetUser)));
        channelRepository.saveAndFlush(channel);
        return new PrivateChannelProfile(channel);
    }

    @Override
    @Transactional(readOnly = true)
    public SliceList<PrivateChannelProfile> getAllChannels(String ofUserId, PageRequest pageRequest) throws UserDoesNotExist {
        org.springframework.data.domain.PageRequest paging = pageValidator.validate(pageRequest);
        User user = userRepository.findById(uuidValidator.validate(ofUserId))
                .orElseThrow(() -> new UserDoesNotExist("user is not exist !"));

        Slice<PrivateChannel> slice = channelRepository.findByIsUserInMembers(user, paging);

        return new SliceList<>(slice.getNumber(), slice.getSize(),
                slice.stream().map(PrivateChannelProfile::new)
                        .collect(Collectors.toList()), slice.hasNext());
    }

    @Override
    @Transactional(readOnly = true)
    public PrivateChannelProfile getChannelProfile(String ofUserId, String channelId) throws UserDoesNotExist, ChannelDoesNotExist, InvalidOperation {
        User user = userRepository.findById(uuidValidator.validate(ofUserId))
                .orElseThrow(() -> new UserDoesNotExist("user is not exist !"));
        PrivateChannel channel = channelRepository.findById(uuidValidator.validate(channelId))
                .orElseThrow(() -> new ChannelDoesNotExist("channel is not exist !"));
        if (!channel.getMembers().contains(user))
            throw new InvalidOperation("user is not in members of the channel !");

        return new PrivateChannelProfile(channel);
    }
}
