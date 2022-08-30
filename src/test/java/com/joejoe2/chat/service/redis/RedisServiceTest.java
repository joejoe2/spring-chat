package com.joejoe2.chat.service.redis;

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import redis.embedded.RedisServer;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class RedisServiceTest {
    @MockBean
    Connection connection;
    @MockBean
    Dispatcher dispatcher;
    @Autowired
    RedisService redisService;
    @Autowired
    StringRedisTemplate redisTemplate;

    private static RedisServer redisServer;

    @BeforeAll
    static void beforeAll() {
        redisServer=RedisServer.builder().port(6370).setting("maxmemory 128M").build();
        redisServer.start();
    }

    @AfterAll
    static void afterAll() {
        redisServer.stop();
    }

    @Test
    void set() {
        redisService.set("key1", "test", Duration.ofSeconds(30));
        assertTrue(redisTemplate.hasKey("key1"));
        assert redisTemplate.getExpire("key1", TimeUnit.SECONDS)<30;
    }

    @Test
    void get() {
        redisTemplate.opsForValue().set("key2", "test", Duration.ofSeconds(30));
        redisService.get("key2").get().equals("test");
    }

    @Test
    void has() {
        redisTemplate.opsForValue().set("key3", "test", Duration.ofSeconds(30));
        assert redisService.has("key3");
    }
}