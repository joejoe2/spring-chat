package com.joejoe2.chat.data.message.request;

import com.joejoe2.chat.validation.constraint.Message;
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
public class PublishPrivateMessageRequest {
  @Schema(description = "id of target channel")
  @UUID(message = "invalid channel id !")
  private String channelId;

  @Schema(description = "content of message")
  @Message
  private String message;
}
