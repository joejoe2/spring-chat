package com.joejoe2.chat.data.message;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.joejoe2.chat.data.UserPublicProfile;
import com.joejoe2.chat.models.MessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.UUID;

@JsonTypeInfo(use=JsonTypeInfo.Id.DEDUCTION)
@Jacksonized
@NoArgsConstructor
@AllArgsConstructor
@Data
public class MessageDto {
    @Schema(description = "version of the message")
    protected Instant version;

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

    public MessageDto(Instant version, UUID id, UUID channel, MessageType messageType,
                      UserPublicProfile from, String content, String createAt, String updateAt) {
        this.version = version;
        this.id = id;
        this.channel = channel;
        this.messageType = messageType;
        this.from = from;
        this.content = content;
        this.createAt = createAt;
        this.updateAt = updateAt;
    }
}
