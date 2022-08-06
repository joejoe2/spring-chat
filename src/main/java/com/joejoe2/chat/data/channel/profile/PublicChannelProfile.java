package com.joejoe2.chat.data.channel.profile;

import com.joejoe2.chat.models.PublicChannel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class PublicChannelProfile {
    @Schema(description = "id of the channel")
    private String id;
    @Schema(description = "name of the channel")
    private String name;

    public PublicChannelProfile(PublicChannel channel){
        id = channel.getId().toString();
        name = channel.getName();
    }
}
