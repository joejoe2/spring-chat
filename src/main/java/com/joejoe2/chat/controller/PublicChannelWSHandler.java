package com.joejoe2.chat.controller;

import com.joejoe2.chat.exception.ChannelDoesNotExist;
import com.joejoe2.chat.service.channel.PublicChannelService;
import com.joejoe2.chat.utils.WebSocketUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class PublicChannelWSHandler extends TextWebSocketHandler {
    @Autowired
    PublicChannelService channelService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        try {
            channelService.subscribe(session, (String) session.getAttributes().getOrDefault("channelId", ""));
        } catch (IllegalArgumentException | ChannelDoesNotExist e) {
            session.close(CloseStatus.BAD_DATA);
        } catch (Exception e) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        exception.printStackTrace();
        WebSocketUtil.executeFinishedCallbacks(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        WebSocketUtil.executeFinishedCallbacks(session);
    }
}
