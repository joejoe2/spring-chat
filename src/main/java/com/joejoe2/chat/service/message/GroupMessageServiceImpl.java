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
import com.joejoe2.chat.repository.user.UserRepository;
import com.joejoe2.chat.service.nats.NatsService;
import com.joejoe2.chat.utils.ChannelSubject;
import com.joejoe2.chat.validation.validator.MessageValidator;
import com.joejoe2.chat.validation.validator.PageRequestValidator;
import com.joejoe2.chat.validation.validator.UUIDValidator;
import java.time.Instant;
import java.util.Comparator;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Slice;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GroupMessageServiceImpl implements GroupMessageService {
  @Autowired UserRepository userRepository;
  @Autowired GroupChannelRepository channelRepository;
  @Autowired GroupMessageRepository messageRepository;
  @Autowired NatsService natsService;
  @Autowired ObjectMapper objectMapper;
  private static final Logger logger = LoggerFactory.getLogger(GroupMessageService.class);

  UUIDValidator uuidValidator = UUIDValidator.getInstance();

  MessageValidator messageValidator = MessageValidator.getInstance();

  PageRequestValidator pageValidator = PageRequestValidator.getInstance();

  @Override
  @Retryable(value = OptimisticLockingFailureException.class, backoff = @Backoff(delay = 100))
  @Transactional(rollbackFor = Exception.class)
  public GroupMessageDto createMessage(String fromUserId, String channelId, String message)
      throws UserDoesNotExist, ChannelDoesNotExist, InvalidOperation {
    message = messageValidator.validate(message);
    User fromUser =
        userRepository
            .findById(uuidValidator.validate(fromUserId))
            .orElseThrow(() -> UserDoesNotExist.ofId(fromUserId));
    GroupChannel channel =
        channelRepository
            .findById(uuidValidator.validate(channelId))
            .orElseThrow(() -> ChannelDoesNotExist.ofId(channelId));

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
    User user =
        userRepository
            .findById(uuidValidator.validate(userId))
            .orElseThrow(() -> UserDoesNotExist.ofId(userId));

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
