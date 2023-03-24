package com.joejoe2.chat.data.channel.request;

import com.joejoe2.chat.validation.constraint.PublicChannelName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePublicChannelRequest {
  @Schema(description = "channel name")
  @PublicChannelName
  private String channelName;
}
