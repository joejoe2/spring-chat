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
import com.joejoe2.chat.repository.user.UserRepository;
import com.joejoe2.chat.service.nats.NatsService;
import com.joejoe2.chat.utils.ChannelSubject;
import com.joejoe2.chat.validation.validator.MessageValidator;
import com.joejoe2.chat.validation.validator.PageRequestValidator;
import com.joejoe2.chat.validation.validator.UUIDValidator;
import java.time.Instant;
import java.util.Comparator;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Slice;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PrivateMessageServiceImpl implements PrivateMessageService {
  @Autowired UserRepository userRepository;
  @Autowired PrivateChannelRepository channelRepository;
  @Autowired PrivateMessageRepository messageRepository;
  @Autowired NatsService natsService;

  UUIDValidator uuidValidator = UUIDValidator.getInstance();
  MessageValidator messageValidator = MessageValidator.getInstance();

  PageRequestValidator pageValidator = PageRequestValidator.getInstance();

  @Retryable(value = OptimisticLockingFailureException.class, backoff = @Backoff(delay = 100))
  @Transactional(rollbackFor = Exception.class)
  @Override
  public PrivateMessageDto createMessage(String fromUserId, String channelId, String message)
      throws UserDoesNotExist, ChannelDoesNotExist, InvalidOperation, BlockedException {
    message = messageValidator.validate(message);
    User fromUser =
        userRepository
            .findById(uuidValidator.validate(fromUserId))
            .orElseThrow(() -> new UserDoesNotExist(fromUserId));
    PrivateChannel channel =
        channelRepository
            .findById(uuidValidator.validate(channelId))
            .orElseThrow(() -> new ChannelDoesNotExist(channelId));

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
    User user =
        userRepository
            .findById(uuidValidator.validate(userId))
            .orElseThrow(() -> new UserDoesNotExist(userId));

    Slice<PrivateMessage> slice = messageRepository.findAllByUser(user, paging);
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
    User user =
        userRepository
            .findById(uuidValidator.validate(userId))
            .orElseThrow(() -> new UserDoesNotExist(userId));
    PrivateChannel channel =
        channelRepository
            .findById(uuidValidator.validate(channelId))
            .orElseThrow(() -> new ChannelDoesNotExist(channelId));
    if (!channel.getMembers().contains(user))
      throw new InvalidOperation("user is not in members of the channel !");

    Slice<PrivateMessage> slice = messageRepository.findAllByChannel(channel, paging);
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
    User user =
        userRepository
            .findById(uuidValidator.validate(userId))
            .orElseThrow(() -> new UserDoesNotExist(userId));

    Slice<PrivateMessage> slice = messageRepository.findAllByUserSince(user, since, paging);
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
    User user =
        userRepository
            .findById(uuidValidator.validate(userId))
            .orElseThrow(() -> new UserDoesNotExist(userId));
    PrivateChannel channel =
        channelRepository
            .findById(uuidValidator.validate(channelId))
            .orElseThrow(() -> new ChannelDoesNotExist(channelId));
    if (!channel.getMembers().contains(user))
      throw new InvalidOperation("user is not in members of the channel !");

    Slice<PrivateMessage> slice = messageRepository.findAllByChannelSince(channel, since, paging);
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
