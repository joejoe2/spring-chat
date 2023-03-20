package com.joejoe2.chat.config;

import com.joejoe2.chat.controller.PrivateChannelWSHandler;
import com.joejoe2.chat.controller.PublicChannelWSHandler;
import com.joejoe2.chat.interceptor.AuthenticatedHandshakeInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    @Autowired
    PublicChannelWSHandler publicChannelWSHandler;
    @Autowired
    PrivateChannelWSHandler privateChannelWSHandler;
    @Autowired
    AuthenticatedHandshakeInterceptor authenticatedHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry
                .addHandler(publicChannelWSHandler, "/ws/channel/public/subscribe")
                .addHandler(privateChannelWSHandler, "/ws/channel/private/subscribe")
                .addInterceptors(authenticatedHandshakeInterceptor)
                .setAllowedOrigins("*");
    }
}
