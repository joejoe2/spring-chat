package com.joejoe2.chat.data.channel.profile;

import com.joejoe2.chat.data.UserPublicProfile;
import com.joejoe2.chat.models.PrivateChannel;
import com.joejoe2.chat.models.User;
import com.joejoe2.chat.utils.TimeUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PrivateChannelProfile {
  @Schema(description = "id of the channel")
  private String id;

  @Schema(description = "members of the channel")
  private List<UserPublicProfile> members;

  @Schema(description = "block state of the channel")
  private Boolean isBlocked;

  private String createAt;
  private String updateAt;

  public PrivateChannelProfile(PrivateChannel channel, User currentUser) {
    this.id = channel.getId().toString();
    this.members =
        channel.getMembers().stream().map(UserPublicProfile::new).collect(Collectors.toList());
    this.isBlocked = channel.isBlocked(channel.anotherMember(currentUser));
    this.createAt = TimeUtil.roundToMicro(channel.getCreateAt()).toString();
    this.updateAt = TimeUtil.roundToMicro(channel.getUpdateAt()).toString();
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
