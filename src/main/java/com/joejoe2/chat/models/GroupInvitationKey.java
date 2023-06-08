package com.joejoe2.chat.models;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@Builder
public class GroupInvitationKey implements Serializable {
  @Column(name = "user_id", nullable = false)
  UUID userId;

  @Column(name = "group_channel_id", nullable = false)
  UUID channelId;

  public GroupInvitationKey(UUID userId, UUID channelId) {
    this.userId = userId;
    this.channelId = channelId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof GroupInvitationKey that)) return false;
    return userId.equals(that.userId) && channelId.equals(that.channelId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, channelId);
  }
}
