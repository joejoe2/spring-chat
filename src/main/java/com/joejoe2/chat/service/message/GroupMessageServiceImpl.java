package com.joejoe2.chat.service.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.joejoe2.chat.data.PageRequest;
import com.joejoe2.chat.data.SliceList;
import com.joejoe2.chat.data.UserPublicProfile;
import com.joejoe2.chat.data.message.GroupMessageDto;
import com.joejoe2.chat.exception.ChannelDoesNotExist;
import com.joejoe2.chat.exception.InvalidOperation;
import com.joejoe2.chat.exception.UserDoesNotExist;
import com.joejoe2.chat.models.GroupChannel;
import com.joejoe2.chat.models.GroupMessage;
import com.joejoe2.chat.models.MessageType;
import com.joejoe2.chat.models.User;
import com.joejoe2.chat.repository.channel.GroupChannelRepository;
import com.joejoe2.chat.repository.message.GroupMessageRepository;
import com.joejoe2.chat.service.nats.NatsService;
import com.joejoe2.chat.service.user.UserService;
import com.joejoe2.chat.utils.ChannelSubject;
import com.joejoe2.chat.validation.validator.MessageValidator;
import com.joejoe2.chat.validation.validator.PageRequestValidator;
import com.joejoe2.chat.validation.validator.UUIDValidator;
import java.time.Instant;
import java.util.Comparator;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Slice;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GroupMessageServiceImpl implements GroupMessageService {
  private final UserService userService;
  private final GroupChannelRepository channelRepository;
  private final GroupMessageRepository messageRepository;
  private final NatsService natsService;
  private final ObjectMapper objectMapper;
  private static final Logger logger = LoggerFactory.getLogger(GroupMessageService.class);

  private final UUIDValidator uuidValidator = UUIDValidator.getInstance();

  private final MessageValidator messageValidator = MessageValidator.getInstance();

  private final PageRequestValidator pageValidator = PageRequestValidator.getInstance();

  public GroupMessageServiceImpl(
      UserService userService,
      GroupChannelRepository channelRepository,
      GroupMessageRepository messageRepository,
      NatsService natsService,
      ObjectMapper objectMapper) {
    this.userService = userService;
    this.channelRepository = channelRepository;
    this.messageRepository = messageRepository;
    this.natsService = natsService;
    this.objectMapper = objectMapper;
  }

  private GroupChannel getChannelById(String channelId) throws ChannelDoesNotExist {
    return channelRepository
        .findById(uuidValidator.validate(channelId))
        .orElseThrow(
            () ->
                new ChannelDoesNotExist(
                    "channel with id=%s does not exist !".formatted(channelId)));
  }

  @Override
  @Retryable(value = OptimisticLockingFailureException.class, backoff = @Backoff(delay = 100))
  @Transactional(rollbackFor = Exception.class)
  public GroupMessageDto createMessage(String fromUserId, String channelId, String message)
      throws UserDoesNotExist, ChannelDoesNotExist, InvalidOperation {
    message = messageValidator.validate(message);
    User fromUser = userService.getUserById(fromUserId);
    GroupChannel channel = getChannelById(channelId);

    channel.addMessage(fromUser, message);
    channelRepository.saveAndFlush(channel);
    return new GroupMessageDto(channel.getLastMessage());
  }

  @Override
  @Async("asyncExecutor")
  public void deliverMessage(GroupMessageDto message) {
    for (UUID memberId : channelRepository.getMembersIdByChannel(message.getChannel())) {
      natsService.publish(ChannelSubject.groupChannelSubject(memberId.toString()), message);
    }
    // also send to invitee or the one just leave channel
    if (MessageType.INVITATION.equals(message.getMessageType())
        || MessageType.LEAVE.equals(message.getMessageType())) {
      try {
        UserPublicProfile user =
            objectMapper.readValue(message.getContent(), UserPublicProfile.class);
        natsService.publish(ChannelSubject.groupChannelSubject(user.getId()), message);
      } catch (JsonProcessingException e) {
        logger.error(e.getMessage());
      }
    }
  }

  @Override
  @Transactional(readOnly = true)
  public SliceList<GroupMessageDto> getAllMessages(
      String userId, String channelId, Instant since, PageRequest pageRequest)
      throws UserDoesNotExist, ChannelDoesNotExist, InvalidOperation {
    if (since == null) throw new IllegalArgumentException("since cannot be null !");
    org.springframework.data.domain.PageRequest paging = pageValidator.validate(pageRequest);
    User user = userService.getUserById(userId);
    GroupChannel channel = getChannelById(channelId);
    if (!channel.getMembers().contains(user))
      throw new InvalidOperation("user is not in members of the channel !");

    Slice<GroupMessage> slice = messageRepository.findAllByChannelSince(channel, since, paging);
    return new SliceList<>(
        slice.getNumber(),
        slice.getSize(),
        slice.getContent().stream()
            .sorted(Comparator.comparing(GroupMessage::getUpdateAt))
            .map(GroupMessageDto::new)
            .toList(),
        slice.hasNext());
  }

  @Override
  @Transactional(readOnly = true)
  public SliceList<GroupMessageDto> getInvitations(
      String userId, Instant since, PageRequest pageRequest) throws UserDoesNotExist {
    if (since == null) throw new IllegalArgumentException("since cannot be null !");
    org.springframework.data.domain.PageRequest paging = pageValidator.validate(pageRequest);
    User user = userService.getUserById(userId);

    Slice<GroupMessage> slice = messageRepository.findInvitations(user, since, paging);
    return new SliceList<>(
        slice.getNumber(),
        slice.getSize(),
        slice.getContent().stream()
            .sorted(Comparator.comparing(GroupMessage::getUpdateAt))
            .map(GroupMessageDto::new)
            .toList(),
        slice.hasNext());
  }
}
