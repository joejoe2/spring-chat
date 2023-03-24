package com.joejoe2.chat.service.channel;

import com.joejoe2.chat.data.PageList;
import com.joejoe2.chat.data.PageRequest;
import com.joejoe2.chat.data.channel.profile.PublicChannelProfile;
import com.joejoe2.chat.exception.AlreadyExist;
import com.joejoe2.chat.exception.ChannelDoesNotExist;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.socket.WebSocketSession;

public interface PublicChannelService {
  /**
   * subscribe to target public channel using Server Sent Event(SSE)
   *
   * @param channelId target channel id
   * @return SseEmitter
   * @throws ChannelDoesNotExist
   */
  SseEmitter subscribe(String channelId) throws ChannelDoesNotExist;

  /**
   * subscribe to target public channel using WebSocket
   *
   * @param session WebSocket session
   * @param channelId target channel id
   * @throws ChannelDoesNotExist
   */
  void subscribe(WebSocketSession session, String channelId) throws ChannelDoesNotExist;

  /**
   * create a new public channel
   *
   * @param channelName
   * @return created public channel
   * @throws AlreadyExist
   */
  PublicChannelProfile createChannel(String channelName) throws AlreadyExist;

  /**
   * get all public channels with page
   *
   * @param pageRequest
   * @return
   */
  PageList<PublicChannelProfile> getAllChannels(PageRequest pageRequest);

  /**
   * get profile of target channel
   *
   * @param channelId id of target channel
   * @return profile of target channel
   * @throws ChannelDoesNotExist
   */
  PublicChannelProfile getChannelProfile(String channelId) throws ChannelDoesNotExist;
}
