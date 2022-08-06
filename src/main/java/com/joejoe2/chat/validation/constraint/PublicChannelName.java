package com.joejoe2.chat.validation.constraint;

import com.joejoe2.chat.validation.validator.PublicChannelNameValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target(ElementType.FIELD)
@Constraint(validatedBy={})
@Retention(RUNTIME)
@Size(min = PublicChannelNameValidator.minLength, message = "length of channel name is at least "+PublicChannelNameValidator.minLength+" !")
@Size(max = PublicChannelNameValidator.maxLength, message = "length of channel name is at most "+PublicChannelNameValidator.maxLength+" !")
@NotEmpty(message = "channel name cannot be empty !")
@Pattern(regexp= PublicChannelNameValidator.REGEX, message = PublicChannelNameValidator.NOT_MATCH_MSG)
public @interface PublicChannelName {
    String message() default PublicChannelNameValidator.NOT_MATCH_MSG;
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
