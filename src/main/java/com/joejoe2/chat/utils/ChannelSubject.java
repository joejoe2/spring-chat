package com.joejoe2.chat.utils;

public class ChannelSubject {
  private static final String PUBLIC_CHANNEL = "chat.channel.public.";
  private static final String PRIVATE_CHANNEL = "chat.channel.private.user.";

  private static final String GROUP_CHANNEL = "chat.channel.GROUP.user.";

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

  public static String groupChannelSubject(String userId) {
    return GROUP_CHANNEL + userId;
  }

  public static String groupChannelUserOfSubject(String subject) {
    return subject.replace(GROUP_CHANNEL, "");
  }
}
