package com.joejoe2.chat.data.channel.request;

import com.joejoe2.chat.validation.constraint.UUID;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.Instant;

@Data
public class SubscribePublicChannelRequest {
    @Parameter(description = "access token in query")
    @NotEmpty
    private String access_token;

    @Parameter(description = "id of target channel")
    @UUID(message = "invalid channel id !")
    private String channelId;
}
