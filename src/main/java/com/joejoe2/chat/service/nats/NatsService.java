package com.joejoe2.chat.service.nats;

import com.joejoe2.chat.data.message.GroupMessageDto;
import com.joejoe2.chat.data.message.PrivateMessageDto;
import com.joejoe2.chat.data.message.PublicMessageDto;
import io.nats.client.MessageHandler;
import io.nats.client.Subscription;

public interface NatsService {
  void publish(String subject, PrivateMessageDto message);

  void publish(String subject, PublicMessageDto message);

  Subscription subscribe(String subject, MessageHandler handler);

  void publish(String subject, GroupMessageDto message);
}
