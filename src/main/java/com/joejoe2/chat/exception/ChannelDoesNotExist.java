package com.joejoe2.chat.exception;

public class ChannelDoesNotExist extends Exception {
  public ChannelDoesNotExist(String message) {
    super(message);
  }

  public static ChannelDoesNotExist ofId(String channelId) {
    return new ChannelDoesNotExist("channel %s does not exist !".formatted(channelId));
  }
}
