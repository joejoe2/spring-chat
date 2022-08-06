package com.joejoe2.chat.validation.validator;


import com.joejoe2.chat.exception.ValidationError;

public abstract class Validator<I, O> {
    public abstract O validate(I data) throws ValidationError;
}
