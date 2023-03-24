package com.joejoe2.chat.service.nats;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.joejoe2.chat.data.message.PrivateMessageDto;
import com.joejoe2.chat.data.message.PublicMessageDto;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.MessageHandler;
import io.nats.client.Subscription;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NatsServiceImpl implements NatsService {
  @Autowired Connection natsConnection;
  @Autowired Dispatcher natsDispatcher;
  @Autowired ObjectMapper objectMapper;

  private static final Logger logger = LoggerFactory.getLogger(NatsService.class);

  public void publish(String subject, String message) {
    natsConnection.publish(subject, message.getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public void publish(String subject, PrivateMessageDto message) {
    try {
      publish(subject, objectMapper.writeValueAsString(message));
    } catch (Exception e) {
      logger.error(e.getMessage());
    }
  }

  @Override
  public void publish(String subject, PublicMessageDto message) {
    try {
      publish(subject, objectMapper.writeValueAsString(message));
    } catch (Exception e) {
      logger.error(e.getMessage());
    }
  }

  @Override
  public Subscription subscribe(String subject, MessageHandler handler) {
    return natsDispatcher.subscribe(subject, handler);
  }
}
