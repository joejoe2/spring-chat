package com.joejoe2.chat.service.channel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.joejoe2.chat.data.PageRequest;
import com.joejoe2.chat.data.SliceList;
import com.joejoe2.chat.data.channel.profile.GroupChannelProfile;
import com.joejoe2.chat.data.message.GroupMessageDto;
import com.joejoe2.chat.exception.ChannelDoesNotExist;
import com.joejoe2.chat.exception.InvalidOperation;
import com.joejoe2.chat.exception.UserDoesNotExist;
import com.joejoe2.chat.models.GroupChannel;
import com.joejoe2.chat.models.GroupMessage;
import com.joejoe2.chat.models.User;
import com.joejoe2.chat.repository.channel.GroupChannelRepository;
import com.joejoe2.chat.repository.user.UserRepository;
import com.joejoe2.chat.utils.ChannelSubject;
import com.joejoe2.chat.utils.SseUtil;
import com.joejoe2.chat.utils.WebSocketUtil;
import com.joejoe2.chat.validation.validator.ChannelNameValidator;
import com.joejoe2.chat.validation.validator.PageRequestValidator;
import com.joejoe2.chat.validation.validator.UUIDValidator;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
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
public class GroupChannelServiceImpl implements GroupChannelService {
  private static final Logger logger = LoggerFactory.getLogger(GroupChannelService.class);
  @Autowired UserRepository userRepository;
  @Autowired GroupChannelRepository channelRepository;
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
                "chat.group.channel.online.users",
                listeningUsers,
                l -> l.values().stream().mapToDouble(Set::size).sum())
            .register(meterRegistry);
  }

  /**
   * create nats dispatcher with shared message handler for all group messages after bean is
   * constructed, the shared message handler will deliver group messages to registered
   * users(subscribers) on this server
   */
  private void initNats(Connection connection) {
    dispatcher =
        connection.createDispatcher(
            (msg) -> {
              try {
                sendToSubscribers(
                    listeningUsers.get(ChannelSubject.groupChannelUserOfSubject(msg.getSubject())),
                    objectMapper.readValue(
                        new String(msg.getData(), StandardCharsets.UTF_8), GroupMessageDto.class));
              } catch (JsonProcessingException e) {
                e.printStackTrace();
              }
            });
  }

  /** deliver group messages to registered users(subscribers) */
  private void sendToSubscribers(Set<Object> subscribers, GroupMessageDto message) {
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

  @Override
  public SseEmitter subscribe(String fromUserId) throws UserDoesNotExist {
    userRepository
        .findById(uuidValidator.validate(fromUserId))
        .orElseThrow(() -> UserDoesNotExist.ofId(fromUserId));

    SseEmitter subscriber = createUserSubscriber(fromUserId);
    SseUtil.sendConnectEvent(subscriber);
    return subscriber;
  }

  @Override
  public void subscribe(WebSocketSession session, String fromUserId) throws UserDoesNotExist {
    userRepository
        .findById(uuidValidator.validate(fromUserId))
        .orElseThrow(() -> UserDoesNotExist.ofId(fromUserId));
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
                dispatcher.unsubscribe(ChannelSubject.groupChannelSubject(userId));
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
            dispatcher.subscribe(ChannelSubject.groupChannelSubject(userId));
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
  @Transactional(rollbackFor = Exception.class)
  public GroupChannelProfile createChannel(String fromUserId, String name) throws UserDoesNotExist {
    User user =
        userRepository
            .findById(uuidValidator.validate(fromUserId))
            .orElseThrow(() -> UserDoesNotExist.ofId(fromUserId));
    name = ChannelNameValidator.getInstance().validate(name);

    GroupChannel channel = new GroupChannel(Set.of(user));
    channel.setName(name);
    channelRepository.saveAndFlush(channel);
    return new GroupChannelProfile(channel);
  }

  @Override
  @Retryable(value = OptimisticLockingFailureException.class, backoff = @Backoff(delay = 100))
  @Transactional(rollbackFor = Exception.class)
  public GroupMessageDto inviteToChannel(String fromUserId, String toUserId, String channelId)
      throws UserDoesNotExist, ChannelDoesNotExist, InvalidOperation {
    User inviter =
        userRepository
            .findById(uuidValidator.validate(fromUserId))
            .orElseThrow(() -> UserDoesNotExist.ofId(fromUserId));
    User invitee =
        userRepository
            .findById(uuidValidator.validate(toUserId))
            .orElseThrow(() -> UserDoesNotExist.ofId(toUserId));
    GroupChannel channel =
        channelRepository
            .findById(uuidValidator.validate(channelId))
            .orElseThrow(() -> ChannelDoesNotExist.ofId(channelId));

    channel.invite(inviter, invitee);
    channelRepository.saveAndFlush(channel);
    GroupMessage invitationMessage = channel.getLastMessage();
    return new GroupMessageDto(invitationMessage);
  }

  @Override
  @Retryable(value = OptimisticLockingFailureException.class, backoff = @Backoff(delay = 100))
  @Transactional(rollbackFor = Exception.class)
  @CacheEvict(
      value = "GroupChannelMembers",
      key = "'GroupChannelMembers:{'+ #channelId.toString() +'}'")
  public GroupMessageDto acceptInvitationOfChannel(String userId, String channelId)
      throws UserDoesNotExist, ChannelDoesNotExist, InvalidOperation {
    User invitee =
        userRepository
            .findById(uuidValidator.validate(userId))
            .orElseThrow(() -> UserDoesNotExist.ofId(userId));
    GroupChannel channel =
        channelRepository
            .findById(uuidValidator.validate(channelId))
            .orElseThrow(() -> ChannelDoesNotExist.ofId(channelId));

    channel.acceptInvitation(invitee);
    channelRepository.saveAndFlush(channel);
    GroupMessage joinMessage = channel.getLastMessage();

    return new GroupMessageDto(joinMessage);
  }

  @Override
  @Retryable(value = OptimisticLockingFailureException.class, backoff = @Backoff(delay = 100))
  @Transactional(rollbackFor = Exception.class)
  @CacheEvict(
      value = "GroupChannelMembers",
      key = "'GroupChannelMembers:{'+ #channelId.toString() +'}'")
  public GroupMessageDto removeFromChannel(String fromUserId, String targetUserId, String channelId)
      throws UserDoesNotExist, ChannelDoesNotExist, InvalidOperation {
    User actor =
        userRepository
            .findById(uuidValidator.validate(fromUserId))
            .orElseThrow(() -> UserDoesNotExist.ofId(fromUserId));
    User target =
        userRepository
            .findById(uuidValidator.validate(targetUserId))
            .orElseThrow(() -> UserDoesNotExist.ofId(targetUserId));
    GroupChannel channel =
        channelRepository
            .findById(uuidValidator.validate(channelId))
            .orElseThrow(() -> ChannelDoesNotExist.ofId(channelId));

    channel.kickOff(actor, target);
    channelRepository.saveAndFlush(channel);
    GroupMessage leaveMessage = channel.getLastMessage();

    return new GroupMessageDto(leaveMessage);
  }

  @Override
  @Retryable(value = OptimisticLockingFailureException.class, backoff = @Backoff(delay = 100))
  @Transactional(rollbackFor = Exception.class)
  @CacheEvict(
      value = "GroupChannelMembers",
      key = "'GroupChannelMembers:{'+ #channelId.toString() +'}'")
  public GroupMessageDto leaveChannel(String userId, String channelId)
      throws UserDoesNotExist, ChannelDoesNotExist, InvalidOperation {
    User user =
        userRepository
            .findById(uuidValidator.validate(userId))
            .orElseThrow(() -> UserDoesNotExist.ofId(userId));
    GroupChannel channel =
        channelRepository
            .findById(uuidValidator.validate(channelId))
            .orElseThrow(() -> ChannelDoesNotExist.ofId(channelId));

    channel.leave(user);
    channelRepository.saveAndFlush(channel);
    GroupMessage leaveMessage = channel.getLastMessage();

    return new GroupMessageDto(leaveMessage);
  }

  @Override
  @Transactional(readOnly = true)
  public SliceList<GroupChannelProfile> getAllChannels(
      String userId, Instant since, PageRequest pageRequest) throws UserDoesNotExist {
    org.springframework.data.domain.PageRequest paging = pageValidator.validate(pageRequest);
    User user =
        userRepository
            .findById(uuidValidator.validate(userId))
            .orElseThrow(() -> UserDoesNotExist.ofId(userId));

    Slice<GroupChannel> slice = channelRepository.findByIsUserInMembers(user, since, paging);

    return new SliceList<>(
        slice.getNumber(),
        slice.getSize(),
        slice.stream().map(GroupChannelProfile::new).collect(Collectors.toList()),
        slice.hasNext());
  }

  @Override
  @Transactional(readOnly = true)
  public GroupChannelProfile getChannelProfile(String userId, String channelId)
      throws UserDoesNotExist, ChannelDoesNotExist, InvalidOperation {
    User user =
        userRepository
            .findById(uuidValidator.validate(userId))
            .orElseThrow(() -> UserDoesNotExist.ofId(userId));
    GroupChannel channel =
        channelRepository
            .findById(uuidValidator.validate(channelId))
            .orElseThrow(() -> ChannelDoesNotExist.ofId(channelId));
    if (!channel.getMembers().contains(user))
      throw new InvalidOperation("user is not in members of the channel !");

    return new GroupChannelProfile(channel);
  }
}
