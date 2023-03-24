package com.joejoe2.chat.interceptor;

import com.joejoe2.chat.utils.AuthUtil;
import com.joejoe2.chat.utils.HttpUtil;
import java.util.Map;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Component
public class AuthenticatedHandshakeInterceptor implements HandshakeInterceptor {
  @Override
  public boolean beforeHandshake(
      ServerHttpRequest request,
      ServerHttpResponse response,
      WebSocketHandler wsHandler,
      Map<String, Object> attributes) {
    if (!AuthUtil.isAuthenticated()) return false;
    attributes.putAll(HttpUtil.splitQuery(request.getURI().getQuery()));
    return true;
  }

  @Override
  public void afterHandshake(
      ServerHttpRequest request,
      ServerHttpResponse response,
      WebSocketHandler wsHandler,
      Exception exception) {}
}
