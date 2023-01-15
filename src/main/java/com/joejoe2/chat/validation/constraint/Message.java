package com.joejoe2.chat.validation.constraint;

import com.joejoe2.chat.validation.validator.MessageValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target(ElementType.FIELD)
@Constraint(validatedBy = {})
@Size(min = MessageValidator.minLength, message = "length of message is at least " + MessageValidator.minLength + " !")
@Size(max = MessageValidator.maxLength, message = "length of message name is at most " + MessageValidator.maxLength + " !")
@NotBlank(message = "message cannot be empty !")
@Retention(RUNTIME)
public @interface Message {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
