package com.joejoe2.chat.models;

import com.joejoe2.chat.exception.BlockedException;
import com.joejoe2.chat.exception.InvalidOperation;
import java.time.Instant;
import java.util.*;
import javax.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

@Data
@NoArgsConstructor
@Entity
@BatchSize(size = 128)
@Table(
    name = "private_channel",
    indexes = {@Index(columnList = "uniqueUserIds"), @Index(columnList = "updateAt DESC")})
public class PrivateChannel extends TimeStampBase {
  @Version
  @Column(nullable = false, columnDefinition = "TIMESTAMP DEFAULT now()")
  private Instant version;

  @ManyToMany
  @BatchSize(size = 128) // for each PrivateChannels->getMembers
  @JoinTable(
      name = "private_channels_users",
      joinColumns = {@JoinColumn(name = "private_channel_id", nullable = false)},
      inverseJoinColumns = {@JoinColumn(name = "user_id", nullable = false)})
  Set<User> members;

  @ManyToMany
  @BatchSize(size = 128)
  @JoinTable(
      name = "private_channels_blockedBy",
      joinColumns = {@JoinColumn(name = "private_channel_id", nullable = false)},
      inverseJoinColumns = {@JoinColumn(name = "user_id", nullable = false)})
  Set<User> blockedBy = new HashSet<>();

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "channel", orphanRemoval = true)
  List<PrivateMessage> messages;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn
  PrivateMessage lastMessage;

  public PrivateChannel(Set<User> members) {
    this.members = members;
  }

  // concat two user ids in sorted to prevent duplicate channel between them
  @Column(unique = true, nullable = false, updatable = false)
  String uniqueUserIds;

  @PrePersist
  void prePersist() {
    // check
    checkNumOfMembers();
    // pre-process before save
    calculateUniqueByUserIds();
  }

  void checkNumOfMembers() {
    if (getMembers().size() != 2)
      throw new RuntimeException("PrivateChannel must contain 2 members !");
  }

  void calculateUniqueByUserIds() {
    List<String> ids =
        members.stream()
            .sorted(Comparator.comparing(User::getId))
            .map(user -> user.getId().toString())
            .toList();
    StringBuilder uniqueIds = new StringBuilder();
    for (String id : ids) uniqueIds.append(id);
    this.uniqueUserIds = uniqueIds.toString();
  }

  public User anotherMember(User member) {
    return getMembers().stream().filter(u -> !member.getId().equals(u.getId())).findFirst().get();
  }

  /**
   * block or unblock the user
   *
   * @param user target user, cannot {@link #addMessage} until unblocked
   * @param isBlock block or unblock
   */
  public void block(User user, boolean isBlock) {
    if (isBlock) {
      blockedBy.add(anotherMember(user));
    } else {
      blockedBy.remove(anotherMember(user));
    }
  }

  /** whether the user is blocked by the other member */
  public boolean isBlocked(User user) {
    return blockedBy.contains(anotherMember(user));
  }

  public void addMessage(User from, String message) throws InvalidOperation, BlockedException {
    if (!members.contains(from))
      throw new InvalidOperation("user is not in members of the channel !");
    if (isBlocked(from)) throw new BlockedException("user has been blocked !");
    PrivateMessage privateMessage = new PrivateMessage(this, from, anotherMember(from), message);
    messages.add(privateMessage);
    lastMessage = privateMessage;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PrivateChannel)) return false;
    PrivateChannel channel = (PrivateChannel) o;
    return Objects.equals(id, channel.id) && Objects.equals(uniqueUserIds, channel.uniqueUserIds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, uniqueUserIds);
  }

  @Override
  public String toString() {
    return "PrivateChannel{"
        + "id="
        + id
        + ", members="
        + members
        + ", lastMessage="
        + lastMessage
        + ", createAt="
        + createAt
        + ", updateAt="
        + updateAt
        + '}';
  }
}
