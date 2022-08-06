package com.joejoe2.chat.data.channel.request;

import com.joejoe2.chat.validation.constraint.UUID;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class CreatePrivateChannelRequest {
    @Schema(description = "id of target user")
    @UUID(message = "invalid user id")
    String targetUserId;
}
