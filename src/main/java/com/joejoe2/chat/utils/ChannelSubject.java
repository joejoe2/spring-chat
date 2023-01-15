package com.joejoe2.chat.utils;

public class ChannelSubject {
    private static final String PUBLIC_CHANNEL = "chat.channel.public.";
    private static final String PRIVATE_CHANNEL = "chat.channel.private.user.";

    public static String publicChannelSubject(String channelId) {
        return PUBLIC_CHANNEL + channelId;
    }

    public static String publicChannelOfSubject(String subject) {
        return subject.replace(PUBLIC_CHANNEL, "");
    }

    public static String privateChannelSubject(String userId) {
        return PRIVATE_CHANNEL + userId;
    }

    public static String privateChannelUserOfSubject(String subject) {
        return subject.replace(PRIVATE_CHANNEL, "");
    }
}
