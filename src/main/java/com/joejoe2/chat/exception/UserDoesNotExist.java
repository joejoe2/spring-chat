package com.joejoe2.chat.exception;

public class UserDoesNotExist extends Exception {
  public UserDoesNotExist(String message) {
    super(message);
  }

  public static UserDoesNotExist ofId(String userId) {
    return new UserDoesNotExist("user %s does not exist !".formatted(userId));
  }
}
