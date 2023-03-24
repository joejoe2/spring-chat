package com.joejoe2.chat.data;

import lombok.Data;

@Data
public class ErrorMessageResponse {
  String message;

  public ErrorMessageResponse(String message) {
    this.message = message;
  }
}
