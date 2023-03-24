package com.joejoe2.chat.data;

import java.util.Map;
import java.util.TreeSet;
import lombok.Data;

@Data
public class InvalidRequestResponse {
  Map<String, TreeSet<String>> errors;

  public InvalidRequestResponse(Map<String, TreeSet<String>> errors) {
    this.errors = errors;
  }
}
