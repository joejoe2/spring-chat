package com.joejoe2.chat.data.channel.request;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscribePrivateChannelRequest {
    @Parameter(description = "access token in query")
    @NotEmpty
    private String access_token;
}
