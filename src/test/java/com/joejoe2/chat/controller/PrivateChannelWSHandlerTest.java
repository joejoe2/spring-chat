package com.joejoe2.chat.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.joejoe2.chat.TestContext;
import com.joejoe2.chat.data.message.request.PublishPrivateMessageRequest;
import com.joejoe2.chat.models.PrivateChannel;
import com.joejoe2.chat.models.User;
import com.joejoe2.chat.repository.channel.PrivateChannelRepository;
import com.joejoe2.chat.repository.user.UserRepository;
import com.joejoe2.chat.utils.JwtUtil;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.net.URI;
import java.security.interfaces.RSAPrivateKey;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(TestContext.class)
public class PrivateChannelWSHandlerTest {
  User user1, user2;
  String accessToken1, accessToken2;
  PrivateChannel channel;

  @Value("${jwt.secret.privateKey}")
  RSAPrivateKey privateKey;

  @Autowired UserRepository userRepository;
  @Autowired PrivateChannelRepository channelRepository;
  @Autowired SimpleMeterRegistry meterRegistry;
  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    user1 = User.builder().id(UUID.randomUUID()).userName("test1").build();
    user2 = User.builder().id(UUID.randomUUID()).userName("test2").build();
    userRepository.save(user1);
    userRepository.save(user2);
    channel = new PrivateChannel(Set.of(user1, user2));
    channelRepository.save(channel);

    Calendar exp = Calendar.getInstance();
    exp.add(Calendar.MINUTE, 10);
    accessToken1 = JwtUtil.generateAccessToken(privateKey, "jti", "issuer", user1, exp);
    accessToken2 = JwtUtil.generateAccessToken(privateKey, "jti", "issuer", user2, exp);
  }

  @AfterEach
  void tearDown() {
    channelRepository.deleteAll();
    userRepository.deleteAll();
  }

  public static class WsClient extends WebSocketClient {
    CountDownLatch messageLatch;
    List<String> messages = new ArrayList<>();

    public WsClient(URI serverUri) {
      super(serverUri);
    }

    public WsClient(URI serverUri, CountDownLatch messageLatch) {
      super(serverUri);
      this.messageLatch = messageLatch;
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {}

    @Override
    public void onMessage(String s) {
      if (messageLatch != null) messageLatch.countDown();
      messages.add(s);
    }

    @Override
    public void onClose(int i, String s, boolean b) {}

    @Override
    public void onError(Exception e) {}
  }

  @Test
  void subscribe() throws Exception {
    String uri =
        "ws://localhost:8081/ws/channel/private/subscribe?access_token="
            + accessToken2
            + "&channelId="
            + channel.getId();
    WsClient client = new WsClient(URI.create(uri), new CountDownLatch(11));
    client.connectBlocking(5, TimeUnit.SECONDS);
    // publish some messages
    PublishPrivateMessageRequest request =
        PublishPrivateMessageRequest.builder()
            .channelId(channel.getId().toString())
            .message("msg")
            .build();
    List<String> messages = new ArrayList<>();
    messages.add("[]");
    for (int i = 0; i < 10; i++) {
      String t =
          mockMvc
              .perform(
                  MockMvcRequestBuilders.post("/api/channel/private/publishMessage")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(objectMapper.writeValueAsString(request))
                      .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken1)
                      .accept(MediaType.APPLICATION_JSON))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();
      messages.add("[" + t + "]");
    }
    // test success
    Thread.sleep(1000);
    assertTrue(client.isOpen());
    assertTrue(client.messageLatch.await(1, TimeUnit.SECONDS));
    assertEquals(messages, client.messages);
    client.close();
  }
}
