package com.joejoe2.chat.utils;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

public class SseUtil {
    public static void addSseCallbacks(SseEmitter sseEmitter, Runnable runnable) {
        sseEmitter.onCompletion(runnable);
    }

    public static void sendConnectEvent(SseEmitter sseEmitter) {
        try {
            sseEmitter.send("[]");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void sendMessageEvent(SseEmitter sseEmitter, Object data) throws IOException {
        sseEmitter.send(List.of(data));
    }
}
