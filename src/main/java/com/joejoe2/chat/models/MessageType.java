package com.joejoe2.chat.models;

public enum MessageType {
  MESSAGE("MESSAGE"),
  INVITATION("INVITATION"),
  JOIN("JOIN"),
  LEAVE("LEAVE"),
  BAN("BAN"),
  UNBAN("UNBAN");

  private final String value;

  MessageType(String role) {
    this.value = role;
  }

  public String toString() {
    return value;
  }
}
