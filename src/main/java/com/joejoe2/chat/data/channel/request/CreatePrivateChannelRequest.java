package com.joejoe2.chat.data.channel.request;

import com.joejoe2.chat.validation.constraint.UUID;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePrivateChannelRequest {
  @Schema(description = "id of target user")
  @UUID(message = "invalid user id")
  @NotNull(message = "targetUserId is missing !")
  String targetUserId;
}
