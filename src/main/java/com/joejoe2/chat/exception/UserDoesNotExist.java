package com.joejoe2.chat.exception;

public class UserDoesNotExist extends Exception {
  public UserDoesNotExist(String userId) {
    super("user %s does not exist !".formatted(userId));
  }
}
