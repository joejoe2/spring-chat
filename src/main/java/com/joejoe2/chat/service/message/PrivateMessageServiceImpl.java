package com.joejoe2.chat.service.message;

import com.joejoe2.chat.data.PageRequest;
import com.joejoe2.chat.data.SliceList;
import com.joejoe2.chat.data.message.PrivateMessageDto;
import com.joejoe2.chat.exception.BlockedException;
import com.joejoe2.chat.exception.ChannelDoesNotExist;
import com.joejoe2.chat.exception.InvalidOperation;
import com.joejoe2.chat.exception.UserDoesNotExist;
import com.joejoe2.chat.models.PrivateChannel;
import com.joejoe2.chat.models.PrivateMessage;
import com.joejoe2.chat.models.User;
import com.joejoe2.chat.repository.channel.PrivateChannelRepository;
import com.joejoe2.chat.repository.message.PrivateMessageRepository;
import com.joejoe2.chat.service.nats.NatsService;
import com.joejoe2.chat.service.user.UserService;
import com.joejoe2.chat.utils.ChannelSubject;
import com.joejoe2.chat.validation.validator.MessageValidator;
import com.joejoe2.chat.validation.validator.PageRequestValidator;
import com.joejoe2.chat.validation.validator.UUIDValidator;
import java.time.Instant;
import java.util.Comparator;
import java.util.UUID;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Slice;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PrivateMessageServiceImpl implements PrivateMessageService {
  private final UserService userService;
  private final PrivateChannelRepository channelRepository;
  private final PrivateMessageRepository messageRepository;
  private final NatsService natsService;

  private final UUIDValidator uuidValidator = UUIDValidator.getInstance();
  private final MessageValidator messageValidator = MessageValidator.getInstance();

  private final PageRequestValidator pageValidator = PageRequestValidator.getInstance();

  public PrivateMessageServiceImpl(
      UserService userService,
      PrivateChannelRepository channelRepository,
      PrivateMessageRepository messageRepository,
      NatsService natsService) {
    this.userService = userService;
    this.channelRepository = channelRepository;
    this.messageRepository = messageRepository;
    this.natsService = natsService;
  }

  private PrivateChannel getChannelById(String channelId) throws ChannelDoesNotExist {
    return channelRepository
        .findById(uuidValidator.validate(channelId))
        .orElseThrow(
            () ->
                new ChannelDoesNotExist(
                    "channel with id=%s does not exist !".formatted(channelId)));
  }

  @Retryable(value = OptimisticLockingFailureException.class, backoff = @Backoff(delay = 100))
  @Transactional(rollbackFor = Exception.class)
  @Override
  public PrivateMessageDto createMessage(String fromUserId, String channelId, String message)
      throws UserDoesNotExist, ChannelDoesNotExist, InvalidOperation, BlockedException {
    message = messageValidator.validate(message);
    User fromUser = userService.getUserById(fromUserId);
    PrivateChannel channel = getChannelById(channelId);

    channel.addMessage(fromUser, message);
    channelRepository.saveAndFlush(channel);
    return new PrivateMessageDto(channel.getLastMessage());
  }

  @Async("asyncExecutor")
  @Override
  public void deliverMessage(PrivateMessageDto message) {
    for (UUID memberId : channelRepository.getMembersIdByChannel(message.getChannel())) {
      natsService.publish(ChannelSubject.privateChannelSubject(memberId.toString()), message);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public SliceList<PrivateMessageDto> getAllMessages(String userId, PageRequest pageRequest)
      throws UserDoesNotExist {
    org.springframework.data.domain.PageRequest paging = pageValidator.validate(pageRequest);
    User user = userService.getUserById(userId);

    Slice<PrivateMessage> slice = messageRepository.findAllByUser(user.getId(), paging);
    return new SliceList<>(
        slice.getNumber(),
        slice.getSize(),
        slice.getContent().stream()
            .sorted(Comparator.comparing(PrivateMessage::getUpdateAt))
            .map(PrivateMessageDto::new)
            .toList(),
        slice.hasNext());
  }

  @Override
  @Transactional(readOnly = true)
  public SliceList<PrivateMessageDto> getAllMessages(
      String userId, String channelId, PageRequest pageRequest)
      throws UserDoesNotExist, ChannelDoesNotExist, InvalidOperation {
    org.springframework.data.domain.PageRequest paging = pageValidator.validate(pageRequest);
    User user = userService.getUserById(userId);
    PrivateChannel channel = getChannelById(channelId);
    if (!channel.getMembers().contains(user))
      throw new InvalidOperation("user is not in members of the channel !");

    Slice<PrivateMessage> slice = messageRepository.findAllByChannel(channel.getId(), paging);
    return new SliceList<>(
        slice.getNumber(),
        slice.getSize(),
        slice.getContent().stream()
            .sorted(Comparator.comparing(PrivateMessage::getUpdateAt))
            .map(PrivateMessageDto::new)
            .toList(),
        slice.hasNext());
  }

  @Override
  @Transactional(readOnly = true)
  public SliceList<PrivateMessageDto> getAllMessages(
      String userId, Instant since, PageRequest pageRequest) throws UserDoesNotExist {
    if (since == null) throw new IllegalArgumentException("since cannot be null !");
    org.springframework.data.domain.PageRequest paging = pageValidator.validate(pageRequest);
    User user = userService.getUserById(userId);

    Slice<PrivateMessage> slice = messageRepository.findAllByUserSince(user.getId(), since, paging);
    return new SliceList<>(
        slice.getNumber(),
        slice.getSize(),
        slice.getContent().stream()
            .sorted(Comparator.comparing(PrivateMessage::getUpdateAt))
            .map(PrivateMessageDto::new)
            .toList(),
        slice.hasNext());
  }

  @Override
  @Transactional(readOnly = true)
  public SliceList<PrivateMessageDto> getAllMessages(
      String userId, String channelId, Instant since, PageRequest pageRequest)
      throws UserDoesNotExist, ChannelDoesNotExist, InvalidOperation {
    if (since == null) throw new IllegalArgumentException("since cannot be null !");
    org.springframework.data.domain.PageRequest paging = pageValidator.validate(pageRequest);
    User user = userService.getUserById(userId);
    PrivateChannel channel = getChannelById(channelId);
    if (!channel.getMembers().contains(user))
      throw new InvalidOperation("user is not in members of the channel !");

    Slice<PrivateMessage> slice =
        messageRepository.findAllByChannelSince(channel.getId(), since, paging);
    return new SliceList<>(
        slice.getNumber(),
        slice.getSize(),
        slice.getContent().stream()
            .sorted(Comparator.comparing(PrivateMessage::getUpdateAt))
            .map(PrivateMessageDto::new)
            .toList(),
        slice.hasNext());
  }
}
