package com.joejoe2.chat.service.channel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.joejoe2.chat.data.PageRequest;
import com.joejoe2.chat.data.SliceList;
import com.joejoe2.chat.data.channel.profile.PrivateChannelProfile;
import com.joejoe2.chat.data.message.PrivateMessageDto;
import com.joejoe2.chat.exception.AlreadyExist;
import com.joejoe2.chat.exception.ChannelDoesNotExist;
import com.joejoe2.chat.exception.InvalidOperation;
import com.joejoe2.chat.exception.UserDoesNotExist;
import com.joejoe2.chat.models.PrivateChannel;
import com.joejoe2.chat.models.User;
import com.joejoe2.chat.repository.channel.PrivateChannelRepository;
import com.joejoe2.chat.repository.user.UserRepository;
import com.joejoe2.chat.utils.ChannelSubject;
import com.joejoe2.chat.utils.SseUtil;
import com.joejoe2.chat.utils.WebSocketUtil;
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
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class PrivateChannelServiceImpl implements PrivateChannelService {
    private static final int MAX_CONNECT_DURATION = 15;
    private static final Logger logger = LoggerFactory.getLogger(PrivateChannelService.class);
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
    Map<String, Set<Object>> listeningUsers = new ConcurrentHashMap<>();
    @Autowired
    Connection connection;
    Dispatcher dispatcher;
    private ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(1);

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
                sendToSubscribers(
                        listeningUsers.get(ChannelSubject.privateChannelUserOfSubject(msg.getSubject())),
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
    private void sendToSubscribers(Set<Object> subscribers, PrivateMessageDto message) {
        List.copyOf(subscribers).parallelStream().forEach((subscriber) -> {
            try {
                if (subscriber instanceof SseEmitter)
                    SseUtil.sendMessageEvent((SseEmitter) subscriber, message);
                else if (subscriber instanceof WebSocketSession)
                    WebSocketUtil.sendMessage(((WebSocketSession) subscriber),
                            objectMapper.writeValueAsString(message));
            } catch (Exception e) {
                e.printStackTrace();
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

    @Override
    public void subscribe(WebSocketSession session, String fromUserId) throws UserDoesNotExist {
        userRepository.findById(uuidValidator.validate(fromUserId))
                .orElseThrow(() -> new UserDoesNotExist("user is not exist !"));
        addUnSubscribeTriggers(fromUserId, session);
        listenToUser(session, fromUserId);
        WebSocketUtil.sendConnectMessage(session);
    }

    /**
     * create SseEmitter instance(subscriber)
     *
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
     *
     * @param userId
     * @param subscriber
     */
    private void addUnSubscribeTriggers(String userId, SseEmitter subscriber) {
        Runnable unSubscribe = createUnSubscribeTrigger(userId, subscriber);
        SseUtil.addSseCallbacks(subscriber, unSubscribe);
        scheduler.schedule(subscriber::complete, MAX_CONNECT_DURATION, TimeUnit.MINUTES);
    }

    /**
     * add UnSubscribe listener to WebSocketSession instance(subscriber), and force
     * unsubscribing after MAX_CONNECT_DURATION MINUTES
     *
     * @param userId
     * @param subscriber
     */
    private void addUnSubscribeTriggers(String userId, WebSocketSession subscriber) {
        Runnable unSubscribe = createUnSubscribeTrigger(userId, subscriber);
        WebSocketUtil.addFinishedCallbacks(subscriber, unSubscribe);
        scheduler.schedule(()-> {
            try {
                subscriber.close();
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                unSubscribe.run();
            }
        }, MAX_CONNECT_DURATION, TimeUnit.MINUTES);
    }

    private Runnable createUnSubscribeTrigger(String userId, Object subscriber){
        return () -> listeningUsers.compute(userId, (key, subscriptions) -> {
            //remove from subscribers
            if (subscriptions != null) subscriptions.remove(subscriber);
            //unsubscribe if no subscriptions
            if (subscriptions == null || subscriptions.isEmpty()) {
                dispatcher.unsubscribe(ChannelSubject.privateChannelSubject(userId));
                subscriptions = null;
            }
            int count = subscriptions==null?0:subscriptions.size();
            logger.info("User "+userId+" now has "+count+" active subscriptions");
            return subscriptions;
        });
    }

    /**
     * register SseEmitter instance(subscriber) and channelId to nats dispatcher
     *
     * @param subscriber
     * @param userId
     */
    private void listenToUser(SseEmitter subscriber, String userId) {
        addToSubscribers(subscriber, userId);
    }

    /**
     * register SseEmitter instance(subscriber) and channelId to nats dispatcher
     *
     * @param subscriber
     * @param userId
     */
    private void listenToUser(WebSocketSession subscriber, String userId) {
        addToSubscribers(subscriber, userId);
    }

    private void addToSubscribers(Object subscriber, String userId){
        listeningUsers.compute(userId, (key, subscribers) -> {
            if (subscribers == null) {
                subscribers = Collections.synchronizedSet(new HashSet<>());
            }
            subscribers.add(subscriber);
            logger.info("User "+userId+" now has "+subscribers.size()+" active subscriptions");
            dispatcher.subscribe(ChannelSubject.privateChannelSubject(userId));
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
