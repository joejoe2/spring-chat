package com.joejoe2.chat.data.channel.request;

import com.joejoe2.chat.validation.constraint.UUID;
import io.swagger.v3.oas.annotations.Parameter;
import javax.validation.constraints.NotEmpty;
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
  private String channelId;
}
