package com.joejoe2.chat.data;

import lombok.Data;

import java.util.Map;
import java.util.TreeSet;

@Data
public class InvalidRequestResponse {
    Map<String, TreeSet<String>> errors;

    public InvalidRequestResponse(Map<String, TreeSet<String>> errors) {
        this.errors = errors;
    }
}
