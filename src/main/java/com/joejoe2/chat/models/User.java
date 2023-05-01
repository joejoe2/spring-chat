package com.joejoe2.chat.models;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import javax.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@BatchSize(size = 128) // for many to one
@Table(name = "account_user")
public class User {
  @Id
  @Column(unique = true, updatable = false, nullable = false)
  private UUID id;

  @Column(unique = true, length = 32, nullable = false)
  private String userName;

  @ManyToMany(mappedBy = "members")
  Set<PrivateChannel> privateChannels;

  @ManyToMany(mappedBy = "members")
  Set<GroupChannel> groupChannels;

  @ManyToMany(mappedBy = "pendingUsers")
  Set<GroupChannel> invitedChannels;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof User user)) return false;
    return Objects.equals(id, user.id) && Objects.equals(userName, user.userName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, userName);
  }

  @Override
  public String toString() {
    return "User{" + "id=" + id + ", userName='" + userName + '\'' + '}';
  }
}
