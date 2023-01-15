package com.joejoe2.chat.exception;

public class ValidationError extends IllegalArgumentException {
    public ValidationError(String msg) {
        super(msg);
    }
}
