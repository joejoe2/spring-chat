package com.joejoe2.chat.validation.validator;

import com.joejoe2.chat.exception.ValidationError;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class PublicChannelNameValidator extends Validator<String, String>{
    public static final int minLength = 3;
    public static final int maxLength = 64;
    public static final String REGEX = "[a-zA-Z0-9]+";
    public static final String NOT_MATCH_MSG = "password can only contain a-z, A-Z, and 0-9 !";

    private static final Pattern pattern=Pattern.compile(REGEX);

    @Override
    public String validate(String data) throws ValidationError {
        String channelName = data;
        if (channelName==null)throw new ValidationError("password can not be null !");
        channelName=channelName.trim();

        if (channelName.length()==0)throw new ValidationError("channelName can not be empty !");
        if (channelName.length()<minLength)throw new ValidationError("the length of channelName is at least "+PublicChannelNameValidator.minLength+" !");
        if (channelName.length()>maxLength)throw new ValidationError("the length of channelName is at most "+PublicChannelNameValidator.maxLength+" !");
        if (!pattern.matcher(channelName).matches()) throw new ValidationError(NOT_MATCH_MSG);

        return channelName;
    }
}
