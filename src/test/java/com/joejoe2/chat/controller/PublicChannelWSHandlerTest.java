package com.joejoe2.chat.controller;

import com.joejoe2.chat.TestContext;
import com.joejoe2.chat.models.PublicChannel;
import com.joejoe2.chat.models.User;
import com.joejoe2.chat.repository.channel.PublicChannelRepository;
import com.joejoe2.chat.repository.user.UserRepository;
import com.joejoe2.chat.utils.JwtUtil;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.security.interfaces.RSAPrivateKey;
import java.util.Calendar;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@ExtendWith(TestContext.class)
class PublicChannelWSHandlerTest {
    User user;
    String accessToken;
    PublicChannel channel;
    @Value("${jwt.secret.privateKey}")
    RSAPrivateKey privateKey;
    @Autowired
    UserRepository userRepository;
    @Autowired
    PublicChannelRepository channelRepository;

    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).userName("test").build();
        userRepository.save(user);
        channel = new PublicChannel();
        channel.setName("test");
        channelRepository.save(channel);
        Calendar exp = Calendar.getInstance();
        exp.add(Calendar.MINUTE, 10);
        accessToken=JwtUtil.generateAccessToken(privateKey, "jti", "issuer", user, exp);
    }

    @AfterEach
    void tearDown() {
        channelRepository.deleteAll();
        userRepository.deleteAll();
    }

    public class WsClient extends WebSocketClient{
        CountDownLatch messageLatch = new CountDownLatch(1);

        public WsClient(URI serverUri) {
            super(serverUri);
        }

        @Override
        public void onOpen(ServerHandshake serverHandshake) {

        }

        @Override
        public void onMessage(String s) {
            messageLatch.countDown();
        }

        @Override
        public void onClose(int i, String s, boolean b) {

        }

        @Override
        public void onError(Exception e) {

        }
    }

    @Test
    void subscribe() throws Exception{
        String uri = "ws://localhost:8081/ws/channel/public/subscribe?access_token="+accessToken
                +"&channelId="+channel.getId();
        WsClient client=new WsClient(URI.create(uri));
        client.connectBlocking(5, TimeUnit.SECONDS);
        //test success
        Thread.sleep(1000);
        assertTrue(client.isOpen());
        assertTrue(client.messageLatch.await(1, TimeUnit.SECONDS));
    }
}