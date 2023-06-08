package com.joejoe2.chat.service.channel;

import com.joejoe2.chat.data.PageRequest;
import com.joejoe2.chat.data.SliceList;
import com.joejoe2.chat.data.channel.profile.PrivateChannelProfile;
import com.joejoe2.chat.exception.AlreadyExist;
import com.joejoe2.chat.exception.ChannelDoesNotExist;
import com.joejoe2.chat.exception.InvalidOperation;
import com.joejoe2.chat.exception.UserDoesNotExist;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.socket.WebSocketSession;

public interface PrivateChannelService {
  /**
   * subscribe to all private channels of target user
   *
   * @param fromUserId id of target user
   * @return SseEmitter
   * @throws UserDoesNotExist
   */
  SseEmitter subscribe(String fromUserId) throws UserDoesNotExist;

  /**
   * subscribe to all private channels of target user
   *
   * @param fromUserId id of target user
   * @param session WebSocketSession
   * @throws UserDoesNotExist
   */
  void subscribe(WebSocketSession session, String fromUserId) throws UserDoesNotExist;

  /**
   * create a private channel between two users(from and to)
   *
   * @param fromUserId id of user1
   * @param toUserId id of user2
   * @return
   * @throws AlreadyExist
   * @throws UserDoesNotExist
   * @throws InvalidOperation if fromUserId==toUserId
   */
  PrivateChannelProfile createChannelBetween(String fromUserId, String toUserId)
      throws AlreadyExist, UserDoesNotExist, InvalidOperation;

  /**
   * get all private channels of target user with page
   *
   * @param ofUserId id of target user
   * @param pageRequest
   * @return
   * @throws UserDoesNotExist
   */
  SliceList<PrivateChannelProfile> getAllChannels(String ofUserId, PageRequest pageRequest)
      throws UserDoesNotExist;

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
  PrivateChannelProfile getChannelProfile(String ofUserId, String channelId)
      throws UserDoesNotExist, ChannelDoesNotExist, InvalidOperation;
}
