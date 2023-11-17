package com.joejoe2.chat.service.channel;

import com.joejoe2.chat.data.PageRequest;
import com.joejoe2.chat.data.SliceList;
import com.joejoe2.chat.data.UserPublicProfile;
import com.joejoe2.chat.data.channel.profile.GroupChannelProfile;
import com.joejoe2.chat.data.message.GroupMessageDto;
import com.joejoe2.chat.exception.ChannelDoesNotExist;
import com.joejoe2.chat.exception.InvalidOperation;
import com.joejoe2.chat.exception.UserDoesNotExist;
import java.time.Instant;
import java.util.List;
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
   * @param fromUserId creator id
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

  /**
   * let user (invitee) join the channel if there is an invitation
   *
   * @param userId invitee id
   * @param channelId channel id
   * @return GroupMessageDto join message
   * @throws UserDoesNotExist
   * @throws ChannelDoesNotExist
   * @throws InvalidOperation
   */
  GroupMessageDto acceptInvitationOfChannel(String userId, String channelId)
      throws UserDoesNotExist, ChannelDoesNotExist, InvalidOperation;

  /**
   * let administrator remove target user from the channel
   *
   * @param adminId user id of the administrator
   * @param targetUserId id of target user
   * @param channelId id of target channel
   * @throws UserDoesNotExist
   * @throws ChannelDoesNotExist
   * @throws InvalidOperation
   */
  GroupMessageDto removeFromChannel(String adminId, String targetUserId, String channelId)
      throws UserDoesNotExist, ChannelDoesNotExist, InvalidOperation;

  /**
   * let user leave the channel
   *
   * @param userId member id
   * @param channelId id of target channel
   * @return GroupMessageDto leave message
   * @throws UserDoesNotExist
   * @throws ChannelDoesNotExist
   * @throws InvalidOperation
   */
  GroupMessageDto leaveChannel(String userId, String channelId)
      throws UserDoesNotExist, ChannelDoesNotExist, InvalidOperation;

  /**
   * let administrator of the channel ban or unban target user
   *
   * @param adminId user id of the administrator
   * @param targetUserId id of target user
   * @param channelId id of target channel
   * @param isBanned ban or unban
   * @throws UserDoesNotExist
   * @throws ChannelDoesNotExist
   * @throws InvalidOperation
   */
  GroupMessageDto editBanned(
      String adminId, String targetUserId, String channelId, boolean isBanned)
      throws UserDoesNotExist, ChannelDoesNotExist, InvalidOperation;

  /**
   * let user get all banned users in the channel
   *
   * @param userId member id
   * @param channelId id of target channel
   * @return list of banned users
   * @throws UserDoesNotExist
   * @throws ChannelDoesNotExist
   * @throws InvalidOperation
   */
  List<UserPublicProfile> getBannedUsers(String userId, String channelId)
      throws UserDoesNotExist, ChannelDoesNotExist, InvalidOperation;

  /**
   * let user get all administrators in the channel
   *
   * @param userId member id
   * @param channelId id of target channel
   * @return list of administrators
   * @throws UserDoesNotExist
   * @throws ChannelDoesNotExist
   * @throws InvalidOperation
   */
  List<UserPublicProfile> getAdministrators(String userId, String channelId)
      throws UserDoesNotExist, ChannelDoesNotExist, InvalidOperation;

  /**
   * let administrator of the channel set target user to be an administrator or not
   *
   * @param adminId user id of the administrator
   * @param targetUserId id of target user
   * @param channelId id of target channel
   * @param isAdmin set target user to be an administrator or not
   * @throws UserDoesNotExist
   * @throws ChannelDoesNotExist
   * @throws InvalidOperation
   */
  void editAdministrator(String adminId, String targetUserId, String channelId, boolean isAdmin)
      throws UserDoesNotExist, ChannelDoesNotExist, InvalidOperation;

  /**
   * get all group channels of target user with page
   *
   * @param userId id of target user
   * @param since filter by updateAt >= since
   * @param pageRequest
   * @return list of channels
   * @throws UserDoesNotExist
   */
  SliceList<GroupChannelProfile> getAllChannels(
      String userId, Instant since, PageRequest pageRequest) throws UserDoesNotExist;

  /**
   * get profile of target channel of target user
   *
   * @param userId id of target user
   * @param channelId id of target channel
   * @return profile of target channel
   * @throws UserDoesNotExist
   * @throws ChannelDoesNotExist
   * @throws InvalidOperation target user is not in members of target channel
   */
  GroupChannelProfile getChannelProfile(String userId, String channelId)
      throws UserDoesNotExist, ChannelDoesNotExist, InvalidOperation;
}
