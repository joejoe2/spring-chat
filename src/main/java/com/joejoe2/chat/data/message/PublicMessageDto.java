package com.joejoe2.chat.data.message;

import com.joejoe2.chat.data.UserPublicProfile;
import com.joejoe2.chat.models.PublicMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

@Jacksonized
@NoArgsConstructor
@Data
public class PublicMessageDto extends MessageDto {
    public PublicMessageDto(PublicMessage message) {
        super(message.getVersion(), message.getId(),
                message.getChannel().getId(), message.getMessageType(),
                UserPublicProfile.builder()
                        .id(message.getFrom().getId().toString())
                        .username(message.getFrom().getUserName()).build(), message.getContent(),
                message.getCreateAt().toString(), message.getUpdateAt().toString());
    }
}
