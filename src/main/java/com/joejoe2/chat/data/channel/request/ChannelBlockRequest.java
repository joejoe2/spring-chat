package com.joejoe2.chat.data.channel.request;

import com.joejoe2.chat.validation.constraint.UUID;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelBlockRequest {
  @Schema(description = "id of target channel")
  @UUID(message = "invalid channel id !")
  @NotNull(message = "channelId is missing !")
  private String channelId;

  @Schema(description = "block state of target channel")
  @NotNull(message = "isBlocked is missing !")
  private Boolean isBlocked;
}
