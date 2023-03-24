package com.joejoe2.chat.data.channel.request;

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
public class SubscribePrivateChannelRequest {
  @Parameter(description = "access token in query")
  @NotEmpty
  private String access_token;
}
