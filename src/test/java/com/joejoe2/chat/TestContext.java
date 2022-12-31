package com.joejoe2.chat;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.GenericContainer;

public class TestContext implements BeforeAllCallback, ExtensionContext.Store.CloseableResource {

    private static boolean started = false;
    static {
        //setup redis for all tests
        setUpRedis("6370");
        //setup nats for all tests
        setUpNats("4221");
    }

    private static GenericContainer setUpRedis(String port){
        GenericContainer redis = new GenericContainer("redis:6.2.7-alpine")
                .withExposedPorts(6379);
        redis.getPortBindings().add(port+":6379");
        redis.start();

        System.setProperty("spring.redis.host", redis.getContainerIpAddress());
        System.setProperty("spring.redis.port", redis.getFirstMappedPort() + "");
        return redis;
    }

    private static GenericContainer setUpNats(String port){
        GenericContainer nats = new GenericContainer("nats:2.8")
                .withExposedPorts(4222, 6222, 8222);
        nats.getPortBindings().add(port+":4222");
        nats.start();

        System.setProperty("nats.url", "nats://"+nats.getContainerIpAddress()+":"+nats.getFirstMappedPort());
        return nats;
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        if (!started) {
            started = true;
            // Your "before all tests" startup logic goes here
        }
    }

    @Override
    public void close() {
        // Your "after all tests" logic goes here
    }
}
