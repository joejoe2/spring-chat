package com.joejoe2.chat.data.channel.request;

import com.joejoe2.chat.validation.constraint.PublicChannelName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class CreatePublicChannelRequest {
    @Schema(description = "channel name")
    @PublicChannelName
    private String channelName;
}
