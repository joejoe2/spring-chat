package com.joejoe2.chat.config;

import com.joejoe2.chat.controller.GroupChannelWSHandler;
import com.joejoe2.chat.controller.PrivateChannelWSHandler;
import com.joejoe2.chat.controller.PublicChannelWSHandler;
import com.joejoe2.chat.interceptor.AuthenticatedHandshakeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
  private final PublicChannelWSHandler publicChannelWSHandler;
  private final PrivateChannelWSHandler privateChannelWSHandler;
  private final GroupChannelWSHandler groupChannelWSHandler;
  private final AuthenticatedHandshakeInterceptor authenticatedHandshakeInterceptor;

  public WebSocketConfig(
      PublicChannelWSHandler publicChannelWSHandler,
      PrivateChannelWSHandler privateChannelWSHandler,
      GroupChannelWSHandler groupChannelWSHandler,
      AuthenticatedHandshakeInterceptor authenticatedHandshakeInterceptor) {
    this.publicChannelWSHandler = publicChannelWSHandler;
    this.privateChannelWSHandler = privateChannelWSHandler;
    this.groupChannelWSHandler = groupChannelWSHandler;
    this.authenticatedHandshakeInterceptor = authenticatedHandshakeInterceptor;
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry
        .addHandler(publicChannelWSHandler, "/ws/channel/public/subscribe")
        .addHandler(privateChannelWSHandler, "/ws/channel/private/subscribe")
        .addHandler(groupChannelWSHandler, "/ws/channel/group/subscribe")
        .addInterceptors(authenticatedHandshakeInterceptor)
        .setAllowedOrigins("*");
  }
}
