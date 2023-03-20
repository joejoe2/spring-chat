package com.joejoe2.chat.data.channel.profile;

import com.joejoe2.chat.data.UserPublicProfile;
import com.joejoe2.chat.models.PrivateChannel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class PrivateChannelProfile {
    @Schema(description = "id of the channel")
    private String id;
    @Schema(description = "members of the channel")
    private List<UserPublicProfile> members;

    private String createAt;
    private String updateAt;

    public PrivateChannelProfile(PrivateChannel channel) {
        this.id = channel.getId().toString();
        this.members = channel.getMembers().stream().map(UserPublicProfile::new).collect(Collectors.toList());
        this.createAt = channel.getCreateAt().toString();
        this.updateAt = channel.getUpdateAt().toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PrivateChannelProfile that)) return false;
        return id.equals(that.id) && members.equals(that.members);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, members);
    }
}
