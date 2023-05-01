package com.joejoe2.chat.service.channel;

import com.joejoe2.chat.data.PageRequest;
import com.joejoe2.chat.data.SliceList;
import com.joejoe2.chat.data.channel.profile.GroupChannelProfile;
import com.joejoe2.chat.data.message.GroupMessageDto;
import com.joejoe2.chat.exception.ChannelDoesNotExist;
import com.joejoe2.chat.exception.InvalidOperation;
import com.joejoe2.chat.exception.UserDoesNotExist;
import java.time.Instant;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.socket.WebSocketSession;

public interface GroupChannelService {
  /**
   * subscribe to all group channels of target user
   *
   * @param fromUserId id of target user
   * @return SseEmitter
   * @throws UserDoesNotExist
   */
  SseEmitter subscribe(String fromUserId) throws UserDoesNotExist;

  /**
   * subscribe to all group channels of target user
   *
   * @param fromUserId id of target user
   * @param session WebSocketSession
   * @throws UserDoesNotExist
   */
  void subscribe(WebSocketSession session, String fromUserId) throws UserDoesNotExist;

  /**
   * create a group channel
   *
   * @param fromUserId creator
   * @return GroupChannelProfile
   * @throws UserDoesNotExist
   */
  GroupChannelProfile createChannel(String fromUserId, String name) throws UserDoesNotExist;

  /**
   * invite an user to the channel
   *
   * @param fromUserId inviter id
   * @param toUserId invitee id
   * @param channelId
   * @return GroupMessageDto invitation message
   * @throws UserDoesNotExist
   * @throws ChannelDoesNotExist
   * @throws InvalidOperation
   */
  GroupMessageDto inviteToChannel(String fromUserId, String toUserId, String channelId)
      throws UserDoesNotExist, ChannelDoesNotExist, InvalidOperation;

  @Transactional(readOnly = true)
  SliceList<String> getInvitedChannels(String ofUserId, Instant since, PageRequest pageRequest)
      throws UserDoesNotExist;

  /**
   * @param ofUserId invitee id
   * @param channelId channel id
   * @return GroupMessageDto join message
   * @throws UserDoesNotExist
   * @throws ChannelDoesNotExist
   * @throws InvalidOperation
   */
  GroupMessageDto acceptInvitationOfChannel(String ofUserId, String channelId)
      throws UserDoesNotExist, ChannelDoesNotExist, InvalidOperation;

  @Retryable(value = OptimisticLockingFailureException.class, backoff = @Backoff(delay = 100))
  @Transactional(rollbackFor = Exception.class)
  GroupMessageDto removeFromChannel(String fromUserId, String targetUserId, String channelId)
      throws UserDoesNotExist, ChannelDoesNotExist, InvalidOperation;

  @Retryable(value = OptimisticLockingFailureException.class, backoff = @Backoff(delay = 100))
  @Transactional(rollbackFor = Exception.class)
  GroupMessageDto leaveChannel(String ofUserId, String channelId)
      throws UserDoesNotExist, ChannelDoesNotExist, InvalidOperation;

  /**
   * get all group channels of target user with page
   *
   * @param ofUserId id of target user
   * @param since filter by updateAt >= since
   * @param pageRequest
   * @throws UserDoesNotExist
   */
  SliceList<GroupChannelProfile> getAllChannels(
      String ofUserId, Instant since, PageRequest pageRequest) throws UserDoesNotExist;

  /**
   * get profile of target channel of target user
   *
   * @param ofUserId id of target user
   * @param channelId id of target channel
   * @return profile of target channel
   * @throws UserDoesNotExist
   * @throws ChannelDoesNotExist
   * @throws InvalidOperation target user is not in members of target channel
   */
  GroupChannelProfile getChannelProfile(String ofUserId, String channelId)
      throws UserDoesNotExist, ChannelDoesNotExist, InvalidOperation;
}
