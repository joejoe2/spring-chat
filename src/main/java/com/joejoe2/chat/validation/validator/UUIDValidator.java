package com.joejoe2.chat.validation.validator;

import com.joejoe2.chat.exception.ValidationError;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UUIDValidator extends Validator<String, UUID> {
    @Override
    public UUID validate(String data) throws ValidationError {
        if (data == null) throw new ValidationError("uuid can not be null !");

        try {
            return UUID.fromString(data);
        } catch (Exception e) {
            throw new ValidationError(e.getMessage());
        }
    }
}
