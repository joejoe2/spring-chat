package com.joejoe2.chat.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.joejoe2.chat.TestContext;
import com.joejoe2.chat.data.message.request.PublishMessageRequest;
import com.joejoe2.chat.models.GroupChannel;
import com.joejoe2.chat.models.User;
import com.joejoe2.chat.repository.channel.GroupChannelRepository;
import com.joejoe2.chat.repository.user.UserRepository;
import com.joejoe2.chat.utils.JwtUtil;
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
public class GroupChannelWSHandlerTest {
  User[] users = new User[3];
  String[] accessTokens = new String[3];
  GroupChannel channel;

  @Value("${jwt.secret.privateKey}")
  RSAPrivateKey privateKey;

  @Autowired UserRepository userRepository;
  @Autowired GroupChannelRepository channelRepository;
  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    Calendar exp = Calendar.getInstance();
    exp.add(Calendar.MINUTE, 10);
    for (int i = 0; i < users.length; i++) {
      users[i] = User.builder().id(UUID.randomUUID()).userName("test" + i).build();
      accessTokens[i] = JwtUtil.generateAccessToken(privateKey, "jti", "issuer", users[i], exp);
    }
    userRepository.saveAll(Arrays.stream(users).toList());
    channel = new GroupChannel(Set.of(users));
    channelRepository.save(channel);
    for (int i = 0; i < users.length; i++) {
      accessTokens[i] = JwtUtil.generateAccessToken(privateKey, "jti", "issuer", users[i], exp);
    }
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
    int messageCount = 5;
    WsClient[] clients = new WsClient[users.length];
    for (int i = 0; i < users.length; i++) {
      String uri =
          "ws://localhost:8081/ws/channel/group/subscribe?access_token="
              + accessTokens[i]
              + "&channelId="
              + channel.getId();
      clients[i] = new WsClient(URI.create(uri), new CountDownLatch(messageCount + 1));
      clients[i].connectBlocking(5, TimeUnit.SECONDS);
    }
    // publish some messages
    PublishMessageRequest request =
        PublishMessageRequest.builder()
            .channelId(channel.getId().toString())
            .message("msg")
            .build();
    List<String> messages = new ArrayList<>();
    messages.add("[]");
    for (int i = 0; i < messageCount; i++) {
      String t =
          mockMvc
              .perform(
                  MockMvcRequestBuilders.post("/api/channel/group/publishMessage")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(objectMapper.writeValueAsString(request))
                      .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessTokens[i % users.length])
                      .accept(MediaType.APPLICATION_JSON))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();
      messages.add("[" + t + "]");
    }
    // test success
    Thread.sleep(1000);
    for (int i = 0; i < users.length; i++) {
      assertTrue(clients[i].isOpen());
      assertTrue(clients[i].messageLatch.await(5, TimeUnit.SECONDS));
      assertEquals(messages, clients[i].messages);
      clients[i].close();
    }
  }
}
