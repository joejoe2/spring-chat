package com.joejoe2.chat.data.message;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.joejoe2.chat.data.UserPublicProfile;
import com.joejoe2.chat.models.MessageType;
import com.joejoe2.chat.utils.TimeUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
@Jacksonized
@NoArgsConstructor
@Data
public class MessageDto {
  @Schema(description = "version of the message")
  protected Double version;

  @Schema(description = "id of the message")
  protected UUID id;

  @Schema(description = "channel id of the message")
  protected UUID channel;

  @Schema(description = "type of the message")
  protected MessageType messageType;

  @Schema(ref = "Sender")
  protected UserPublicProfile from;

  @Schema(ref = "Receiver")
  protected UserPublicProfile to;

  @Schema(description = "content of the message")
  protected String content;

  @Schema(description = "when is the message created")
  protected String createAt;

  @Schema(description = "when is the message updated")
  protected String updateAt;

  public MessageDto(
      Instant version,
      UUID id,
      UUID channel,
      MessageType messageType,
      UserPublicProfile from,
      String content,
      String createAt,
      String updateAt) {
    this.version =
        ChronoUnit.MICROS.between(Instant.EPOCH, TimeUtil.roundToMicro(version)) / 1000.0;
    this.id = id;
    this.channel = channel;
    this.messageType = messageType;
    this.from = from;
    this.content = content;
    this.createAt = createAt;
    this.updateAt = updateAt;
  }

  public MessageDto(
      Instant version,
      UUID id,
      UUID channel,
      MessageType messageType,
      UserPublicProfile from,
      UserPublicProfile to,
      String content,
      String createAt,
      String updateAt) {
    this.version =
        ChronoUnit.MICROS.between(Instant.EPOCH, TimeUtil.roundToMicro(version)) / 1000.0;
    this.id = id;
    this.channel = channel;
    this.messageType = messageType;
    this.from = from;
    this.to = to;
    this.content = content;
    this.createAt = createAt;
    this.updateAt = updateAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof MessageDto that)) return false;
    return version.equals(that.version)
        && id.equals(that.id)
        && channel.equals(that.channel)
        && messageType == that.messageType
        && from.equals(that.from)
        && Objects.equals(to, that.to)
        && content.equals(that.content)
        && createAt.equals(that.createAt)
        && updateAt.equals(that.updateAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(version, id, channel, messageType, from, to, content, createAt, updateAt);
  }
}
