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
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Slice;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Service
public class PrivateChannelServiceImpl implements PrivateChannelService {
  private static final Logger logger = LoggerFactory.getLogger(PrivateChannelService.class);
  @Autowired UserRepository userRepository;
  @Autowired PrivateChannelRepository channelRepository;
  @Autowired ObjectMapper objectMapper;

  UUIDValidator uuidValidator = UUIDValidator.getInstance();

  PageRequestValidator pageValidator = PageRequestValidator.getInstance();
  Map<String, Set<Object>> listeningUsers = new ConcurrentHashMap<>();
  @Autowired Connection connection;
  Dispatcher dispatcher;
  private final Executor sendingScheduler = Executors.newFixedThreadPool(5);

  @Autowired MeterRegistry meterRegistry;

  Gauge onlineUsers;

  @PostConstruct
  private void afterInjection() {
    initMetrics(meterRegistry);
    initNats(connection);
  }

  private void initMetrics(MeterRegistry meterRegistry) {
    onlineUsers =
        Gauge.builder(
                "chat.private.channel.online.users",
                listeningUsers,
                l -> l.values().stream().mapToDouble(Set::size).sum())
            .register(meterRegistry);
  }

  /**
   * create nats dispatcher with shared message handler for all private messages after bean is
   * constructed, the shared message handler will deliver private messages to registered
   * users(subscribers) on this server
   */
  private void initNats(Connection connection) {
    dispatcher =
        connection.createDispatcher(
            (msg) -> {
              try {
                sendToSubscribers(
                    listeningUsers.get(
                        ChannelSubject.privateChannelUserOfSubject(msg.getSubject())),
                    objectMapper.readValue(
                        new String(msg.getData(), StandardCharsets.UTF_8),
                        PrivateMessageDto.class));
              } catch (JsonProcessingException e) {
                e.printStackTrace();
              }
            });
  }

  /** deliver private messages to registered users(subscribers) */
  private void sendToSubscribers(Set<Object> subscribers, PrivateMessageDto message) {
    sendingScheduler.execute(
        () -> {
          try {
            TextMessage textMessage =
                new TextMessage("[" + objectMapper.writeValueAsString(message) + "]");
            for (Object subscriber : subscribers.toArray()) {
              if (subscriber instanceof SseEmitter)
                SseUtil.sendMessageEvent((SseEmitter) subscriber, textMessage);
              else if (subscriber instanceof WebSocketSession)
                WebSocketUtil.sendMessage(((WebSocketSession) subscriber), textMessage);
            }
          } catch (Exception e) {
            e.printStackTrace();
          }
        });
  }

  private User getUserById(String userId) throws UserDoesNotExist {
    return userRepository
        .findById(uuidValidator.validate(userId))
        .orElseThrow(
            () -> new UserDoesNotExist("user with id=%s does not exist !".formatted(userId)));
  }

  private PrivateChannel getChannelById(String channelId) throws ChannelDoesNotExist {
    return channelRepository
        .findById(uuidValidator.validate(channelId))
        .orElseThrow(
            () ->
                new ChannelDoesNotExist(
                    "channel with id=%s does not exist !".formatted(channelId)));
  }

  @Override
  public SseEmitter subscribe(String fromUserId) throws UserDoesNotExist {
    getUserById(fromUserId);

    SseEmitter subscriber = createUserSubscriber(fromUserId);
    SseUtil.sendConnectEvent(subscriber);
    return subscriber;
  }

  @Override
  public void subscribe(WebSocketSession session, String fromUserId) throws UserDoesNotExist {
    getUserById(fromUserId);
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
   * add UnSubscribe listener to SseEmitter instance(subscriber), and force unsubscribing after
   * MAX_CONNECT_DURATION MINUTES
   *
   * @param userId
   * @param subscriber
   */
  private void addUnSubscribeTriggers(String userId, SseEmitter subscriber) {
    Runnable unSubscribe = createUnSubscribeTrigger(userId, subscriber);
    SseUtil.addSseCallbacks(subscriber, unSubscribe);
  }

  /**
   * add UnSubscribe listener to WebSocketSession instance(subscriber), and force unsubscribing
   * after MAX_CONNECT_DURATION MINUTES
   *
   * @param userId
   * @param subscriber
   */
  private void addUnSubscribeTriggers(String userId, WebSocketSession subscriber) {
    Runnable unSubscribe = createUnSubscribeTrigger(userId, subscriber);
    WebSocketUtil.addFinishedCallbacks(subscriber, unSubscribe);
  }

  private Runnable createUnSubscribeTrigger(String userId, Object subscriber) {
    return () ->
        listeningUsers.compute(
            userId,
            (key, subscriptions) -> {
              // remove from subscribers
              if (subscriptions != null) subscriptions.remove(subscriber);
              // unsubscribe if no subscriptions
              if (subscriptions == null || subscriptions.isEmpty()) {
                dispatcher.unsubscribe(ChannelSubject.privateChannelSubject(userId));
                subscriptions = null;
              }
              // decrease online user
              int count = subscriptions == null ? 0 : subscriptions.size();
              logger.info("User " + userId + " now has " + count + " active subscriptions");
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

  private void addToSubscribers(Object subscriber, String userId) {
    listeningUsers.compute(
        userId,
        (key, subscribers) -> {
          if (subscribers == null) {
            dispatcher.subscribe(ChannelSubject.privateChannelSubject(userId));
            subscribers = Collections.synchronizedSet(new HashSet<>());
          }
          subscribers.add(subscriber);
          // increase online users
          logger.info(
              "User " + userId + " now has " + subscribers.size() + " active subscriptions");
          return subscribers;
        });
  }

  @Override
  public PrivateChannelProfile createChannelBetween(String fromUserId, String toUserId)
      throws UserDoesNotExist, AlreadyExist, InvalidOperation {
    User user = getUserById(fromUserId);
    User targetUser = getUserById(toUserId);
    if (user.equals(targetUser)) throw new InvalidOperation("cannot chat with yourself !");
    if (channelRepository.isPrivateChannelExistBetween(user, targetUser))
      throw new AlreadyExist(
          "channel between "
              + user.getUserName()
              + " and "
              + targetUser.getUserName()
              + " is already exist !");

    PrivateChannel channel = new PrivateChannel(new HashSet<>(Arrays.asList(user, targetUser)));
    channelRepository.saveAndFlush(channel);
    return new PrivateChannelProfile(channel, user);
  }

  @Override
  @Transactional(readOnly = true)
  public SliceList<PrivateChannelProfile> getAllChannels(String userId, PageRequest pageRequest)
      throws UserDoesNotExist {
    org.springframework.data.domain.PageRequest paging = pageValidator.validate(pageRequest);
    User user = getUserById(userId);

    Slice<PrivateChannel> slice = channelRepository.findByIsUserInMembers(user, paging);

    return new SliceList<>(
        slice.getNumber(),
        slice.getSize(),
        slice.stream()
            .map((ch) -> new PrivateChannelProfile(ch, user))
            .collect(Collectors.toList()),
        slice.hasNext());
  }

  @Override
  @Transactional(readOnly = true)
  public PrivateChannelProfile getChannelProfile(String userId, String channelId)
      throws UserDoesNotExist, ChannelDoesNotExist, InvalidOperation {
    User user = getUserById(userId);
    PrivateChannel channel = getChannelById(channelId);
    if (!channel.getMembers().contains(user))
      throw new InvalidOperation("user is not in members of the channel !");

    return new PrivateChannelProfile(channel, user);
  }

  @Override
  @Transactional(readOnly = true)
  public SliceList<PrivateChannelProfile> getChannelsBlockedByUser(
      String userId, PageRequest pageRequest) throws UserDoesNotExist {
    org.springframework.data.domain.PageRequest paging = pageValidator.validate(pageRequest);
    User user = getUserById(userId);

    Slice<PrivateChannel> slice = channelRepository.findBlockedByUser(user, paging);

    return new SliceList<>(
        slice.getNumber(),
        slice.getSize(),
        slice.stream()
            .map((ch) -> new PrivateChannelProfile(ch, user))
            .collect(Collectors.toList()),
        slice.hasNext());
  }

  @Override
  @Retryable(value = OptimisticLockingFailureException.class, backoff = @Backoff(delay = 100))
  @Transactional(rollbackFor = Exception.class)
  public void block(String userId, String channelId, boolean isBlock)
      throws UserDoesNotExist, ChannelDoesNotExist, InvalidOperation {
    User user = getUserById(userId);
    PrivateChannel channel = getChannelById(channelId);
    if (!channel.getMembers().contains(user))
      throw new InvalidOperation("user is not in members of the channel !");

    channel.block(channel.anotherMember(user), isBlock);
    channelRepository.save(channel);
  }
}
