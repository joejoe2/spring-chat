package com.joejoe2.chat.exception;

public class ChannelDoesNotExist extends Exception {
  public ChannelDoesNotExist(String channelId) {
    super("channel %s does not exist !".formatted(channelId));
  }
}
