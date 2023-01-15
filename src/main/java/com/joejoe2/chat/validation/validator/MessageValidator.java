package com.joejoe2.chat.validation.validator;

import com.joejoe2.chat.exception.ValidationError;
import org.springframework.stereotype.Component;

@Component
public class MessageValidator extends Validator<String, String> {
    public static final int minLength = 1;
    public static final int maxLength = 4096;

    @Override
    public String validate(String data) throws ValidationError {
        String message = data;
        if (message == null) throw new ValidationError("message can not be null !");
        message = message.trim();

        if (message.length() == 0) throw new ValidationError("message can not be empty !");
        if (message.length() > maxLength)
            throw new ValidationError("the length of message is at most " + maxLength + " !");

        return message;
    }
}
