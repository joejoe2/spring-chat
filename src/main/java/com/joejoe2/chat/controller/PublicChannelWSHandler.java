package com.joejoe2.chat.controller;

import com.joejoe2.chat.exception.ChannelDoesNotExist;
import com.joejoe2.chat.service.channel.PublicChannelService;
import com.joejoe2.chat.utils.WebSocketUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class PublicChannelWSHandler extends TextWebSocketHandler {
  @Autowired PublicChannelService channelService;

  @Override
  public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    WebSocketSession webSocketSession =
        new ConcurrentWebSocketSessionDecorator(session, 5000, 1024 * 512);
    try {
      channelService.subscribe(
          webSocketSession,
          (String) webSocketSession.getAttributes().getOrDefault("channelId", ""));
    } catch (IllegalArgumentException | ChannelDoesNotExist e) {
      webSocketSession.close(CloseStatus.BAD_DATA);
    } catch (Exception e) {
      webSocketSession.close(CloseStatus.SERVER_ERROR);
    }
  }

  @Override
  public void handleTransportError(WebSocketSession session, Throwable exception) {
    exception.printStackTrace();
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    WebSocketUtil.executeFinishedCallbacks(session);
  }
}
