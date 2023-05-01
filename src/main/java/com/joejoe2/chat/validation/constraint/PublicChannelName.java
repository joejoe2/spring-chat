package com.joejoe2.chat.validation.constraint;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.joejoe2.chat.validation.validator.ChannelNameValidator;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Target(ElementType.FIELD)
@Constraint(validatedBy = {})
@Retention(RUNTIME)
@Size(
    min = ChannelNameValidator.minLength,
    message = "length of channel name is at least " + ChannelNameValidator.minLength + " !")
@Size(
    max = ChannelNameValidator.maxLength,
    message = "length of channel name is at most " + ChannelNameValidator.maxLength + " !")
@NotEmpty(message = "channel name cannot be empty !")
@Pattern(regexp = ChannelNameValidator.REGEX, message = ChannelNameValidator.NOT_MATCH_MSG)
public @interface PublicChannelName {
  String message() default ChannelNameValidator.NOT_MATCH_MSG;

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
