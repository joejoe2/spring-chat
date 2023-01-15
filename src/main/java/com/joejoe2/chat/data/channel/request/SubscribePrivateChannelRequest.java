package com.joejoe2.chat.data.channel.request;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;

import javax.validation.constraints.NotEmpty;

@Data
public class SubscribePrivateChannelRequest {
    @Parameter(description = "access token in query")
    @NotEmpty
    private String access_token;
}
