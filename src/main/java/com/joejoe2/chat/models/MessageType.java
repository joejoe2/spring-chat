package com.joejoe2.chat.models;

public enum MessageType {
  MESSAGE("MESSAGE");
  private final String value;

  MessageType(String role) {
    this.value = role;
  }

  public String toString() {
    return value;
  }
}
