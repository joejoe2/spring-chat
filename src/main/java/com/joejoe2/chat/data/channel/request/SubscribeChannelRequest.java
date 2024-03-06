package com.joejoe2.chat.data.channel.request;

import com.joejoe2.chat.validation.constraint.UUID;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscribeChannelRequest {
  @Parameter(description = "access token in query")
  @NotEmpty
  private String access_token;

  @Parameter(description = "id of target channel")
  @UUID(message = "invalid channel id !")
  @NotNull(message = "channelId is missing !")
  private String channelId;
}
