package com.joejoe2.chat.data.channel.request;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.Instant;

@Data
public class SubscribePrivateChannelRequest {
    @Parameter(description = "access token in query")
    @NotEmpty
    private String access_token;
}
