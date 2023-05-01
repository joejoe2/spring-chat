package com.joejoe2.chat.data.channel.request;

import com.joejoe2.chat.validation.constraint.UUID;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelUserRequest {
  @Schema(description = "id of target channel")
  @UUID(message = "invalid channel id !")
  private String channelId;

  @Schema(description = "id of target user")
  @UUID(message = "invalid user id")
  String targetUserId;
}
