package com.joejoe2.chat.service.nats;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.joejoe2.chat.data.message.GroupMessageDto;
import com.joejoe2.chat.data.message.PrivateMessageDto;
import com.joejoe2.chat.data.message.PublicMessageDto;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.MessageHandler;
import io.nats.client.Subscription;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NatsServiceImpl implements NatsService {
  private final Connection natsConnection;
  private final Dispatcher natsDispatcher;
  private final ObjectMapper objectMapper;

  private static final Logger logger = LoggerFactory.getLogger(NatsService.class);

  public NatsServiceImpl(
      Connection natsConnection, Dispatcher natsDispatcher, ObjectMapper objectMapper) {
    this.natsConnection = natsConnection;
    this.natsDispatcher = natsDispatcher;
    this.objectMapper = objectMapper;
  }

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
  public void publish(String subject, GroupMessageDto message) {
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
