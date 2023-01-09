package com.joejoe2.chat.service.channel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.joejoe2.chat.data.PageList;
import com.joejoe2.chat.data.channel.profile.PublicChannelProfile;
import com.joejoe2.chat.data.message.PublicMessageDto;
import com.joejoe2.chat.exception.AlreadyExist;
import com.joejoe2.chat.exception.ChannelDoesNotExist;
import com.joejoe2.chat.models.PublicChannel;
import com.joejoe2.chat.repository.channel.PublicChannelRepository;
import com.joejoe2.chat.utils.ChannelSubject;
import com.joejoe2.chat.utils.SseUtil;
import com.joejoe2.chat.utils.WebSocketUtil;
import com.joejoe2.chat.validation.validator.PageRequestValidator;
import com.joejoe2.chat.validation.validator.PublicChannelNameValidator;
import com.joejoe2.chat.validation.validator.UUIDValidator;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
public class PublicChannelServiceImpl implements PublicChannelService {
    @Autowired
    PublicChannelRepository channelRepository;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    UUIDValidator uuidValidator;
    @Autowired
    PublicChannelNameValidator channelNameValidator;
    @Autowired
    PageRequestValidator pageValidator;

    Map<String, Set<Object>> listeningChannels = new ConcurrentHashMap<>();
    private static final int MAX_CONNECT_DURATION = 15;
    private ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(1);

    private static final Logger logger = LoggerFactory.getLogger(PublicChannelService.class);

    @Autowired
    Connection connection;

    Dispatcher dispatcher;

    /**
     * create nats dispatcher with shared message handler for all
     * public messages after bean is constructed, the shared message
     * handler will deliver public messages to registered subscribers
     * on this server
     */
    @PostConstruct
    private void initNats() {
        dispatcher = connection.createDispatcher((msg) -> {
            try {
                String channel = ChannelSubject.publicChannelOfSubject(msg.getSubject());
                sendToSubscribers(listeningChannels.get(channel),
                        objectMapper.readValue(new String(msg.getData(), StandardCharsets.UTF_8),
                                PublicMessageDto.class));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * deliver public messages to registered subscribers
     */
    private void sendToSubscribers(Set<Object> subscribers, PublicMessageDto message) {
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
    public SseEmitter subscribe(String channelId) throws ChannelDoesNotExist {
        channelRepository.findById(uuidValidator.validate(channelId))
                .orElseThrow(() -> new ChannelDoesNotExist("channel is not exist !"));

        SseEmitter subscriber = createChannelSubscriber(channelId);
        SseUtil.sendConnectEvent(subscriber);
        return subscriber;
    }

    @Override
    public void subscribe(WebSocketSession session, String channelId) throws ChannelDoesNotExist {
        channelRepository.findById(uuidValidator.validate(channelId))
                .orElseThrow(() -> new ChannelDoesNotExist("channel is not exist !"));
        addUnSubscribeTriggers(channelId, session);
        listenToChannel(session, channelId);
        WebSocketUtil.sendConnectMessage(session);
    }

    /**
     * create SseEmitter instance(subscriber)
     * @param channelId
     * @return
     */
    private SseEmitter createChannelSubscriber(String channelId) {
        SseEmitter subscriber = new SseEmitter(120000L);
        addUnSubscribeTriggers(channelId, subscriber);
        listenToChannel(subscriber, channelId);
        return subscriber;
    }

    /**
     * add UnSubscribe listener to SseEmitter instance(subscriber), and force
     * unsubscribing after MAX_CONNECT_DURATION MINUTES
     * @param channelId
     * @param subscriber
     */
    private void addUnSubscribeTriggers(String channelId, SseEmitter subscriber){
        Runnable unSubscribe = createUnSubscribeTrigger(channelId, subscriber);
        SseUtil.addSseCallbacks(subscriber, unSubscribe);
        scheduler.schedule(subscriber::complete, MAX_CONNECT_DURATION, TimeUnit.MINUTES);
    }

    /**
     * add UnSubscribe listener to WebSocketSession instance(subscriber), and force
     * unsubscribing after MAX_CONNECT_DURATION MINUTES
     * @param channelId
     * @param subscriber
     */
    private void addUnSubscribeTriggers(String channelId, WebSocketSession subscriber){
        Runnable unSubscribe = createUnSubscribeTrigger(channelId, subscriber);
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

    private Runnable createUnSubscribeTrigger(String channelId, Object subscriber){
        return () -> listeningChannels.compute(channelId, (key, subscribers) -> {
            if (subscribers != null) subscribers.remove(subscriber);
            if (subscribers == null || subscribers.isEmpty()) {
                dispatcher.unsubscribe(ChannelSubject.publicChannelSubject(channelId));
                subscribers = null;
            }
            int count = subscribers==null?0:subscribers.size();
            logger.info("PublicChannel "+channelId+" now has "+count+" subscribers");
            return subscribers;
        });
    }

    /**
     * register SseEmitter instance(subscriber) and channelId to nats dispatcher
     * @param subscriber
     * @param channelId
     */
    private void listenToChannel(SseEmitter subscriber, String channelId) {
        addToSubscribers(subscriber, channelId);
    }

    /**
     * register WebSocketSession instance(subscriber) and channelId to nats dispatcher
     * @param subscriber
     * @param channelId
     */
    private void listenToChannel(WebSocketSession subscriber, String channelId) {
        addToSubscribers(subscriber, channelId);
    }

    private void addToSubscribers(Object subscriber, String channelId){
        listeningChannels.compute(channelId, (key, subscribers) -> {
            //create new subscribers set
            if (subscribers == null) {
                subscribers = Collections.synchronizedSet(new HashSet<>());
            }
            //add to subscribers
            subscribers.add(subscriber);
            logger.info("PublicChannel "+channelId+" now has "+subscribers.size()+" subscribers");
            //subscribe to nats
            dispatcher.subscribe(ChannelSubject.publicChannelSubject(channelId));
            return subscribers;
        });
    }

    @Override
    public PublicChannelProfile createChannel(String channelName) throws AlreadyExist {
        if (channelRepository.findByName(channelNameValidator.validate(channelName)).isPresent())
            throw new AlreadyExist("channel name is already exist !");

        PublicChannel channel = new PublicChannel(channelName);
        channelRepository.saveAndFlush(channel);
        return new PublicChannelProfile(channel);
    }

    @Override
    @Transactional(readOnly = true)
    public PageList<PublicChannelProfile> getAllChannels(com.joejoe2.chat.data.PageRequest pageRequest) {
        PageRequest paging = pageValidator.validate(pageRequest);
        Page<PublicChannel> page = channelRepository
                .findAll(paging.withSort(Sort.by(Sort.Direction.ASC, "name")));
        List<PublicChannelProfile> profiles = page.getContent().stream()
                .map((PublicChannelProfile::new)).collect(Collectors.toList());

        return new PageList<>(page.getTotalElements(), page.getNumber(),
                page.getTotalPages(), page.getSize(), profiles);
    }

    @Override
    @Transactional(readOnly = true)
    public PublicChannelProfile getChannelProfile(String channelId) throws ChannelDoesNotExist {
        PublicChannel channel = channelRepository.findById(uuidValidator.validate(channelId))
                .orElseThrow(() -> new ChannelDoesNotExist("channel is not exist !"));

        return new PublicChannelProfile(channel);
    }
}
