package com.joejoe2.chat.utils;

import java.io.IOException;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

public class WebSocketUtil {
  public static void addFinishedCallbacks(WebSocketSession session, Runnable runnable) {
    session.getAttributes().put("finishedCallbacks", runnable);
  }

  public static void executeFinishedCallbacks(WebSocketSession session) {
    if (session.getAttributes().containsKey("finishedCallbacks")) {
      ((Runnable) session.getAttributes().get("finishedCallbacks")).run();
    }
  }

  public static void sendConnectMessage(WebSocketSession session) {
    try {
      session.sendMessage(new TextMessage("[]"));
    } catch (IOException e) {
      e.printStackTrace();
      executeFinishedCallbacks(session);
    }
  }

  public static void sendMessage(WebSocketSession session, String msg) throws IOException {
    session.sendMessage(new TextMessage("[" + msg + "]"));
  }
}
