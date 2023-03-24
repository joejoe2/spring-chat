package com.joejoe2.chat.config;

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Nats;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NatsConfig {
  @Value("${nats.url}")
  private String natsUrl;

  @Bean
  Connection natsConnection() throws IOException, InterruptedException {
    return Nats.connect(natsUrl);
  }

  @Bean
  Dispatcher dispatcher(Connection natsConnection) {
    return natsConnection.createDispatcher((msg) -> {});
  }
}
