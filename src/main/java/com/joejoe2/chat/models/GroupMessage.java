package com.joejoe2.chat.models;

import java.time.Instant;
import java.util.Objects;
import javax.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(
    name = "group_message",
    indexes = {
      @Index(columnList = "from_id"),
      @Index(columnList = "channel_id"),
      @Index(columnList = "updateAt DESC")
    })
@BatchSize(size = 32)
public class GroupMessage extends TimeStampBase {
  @Version
  @Column(nullable = false, columnDefinition = "TIMESTAMP DEFAULT now()")
  private Instant version;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @OnDelete(action = OnDeleteAction.CASCADE)
  private GroupChannel channel;

  @Column(length = 32, nullable = false)
  @Enumerated(EnumType.STRING)
  private MessageType messageType = MessageType.MESSAGE; // code level default

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  User from;

  @Column(columnDefinition = "TEXT", nullable = false)
  private String content;

  public GroupMessage(GroupChannel channel, User from, String content) {
    this.channel = channel;
    this.from = from;
    this.content = content;
  }

  public static GroupMessage inviteMessage(GroupChannel channel, User inviter, User invitee) {
    GroupMessage message = new GroupMessage();
    message.channel = channel;
    message.from = inviter;
    message.content =
        "{\"id\":\"%s\", \"username\":\"%s\"}"
            .formatted(invitee.getId().toString(), invitee.getUserName());
    message.messageType = MessageType.INVITATION;
    return message;
  }

  public static GroupMessage joinMessage(GroupChannel channel, User joiner) {
    GroupMessage message = new GroupMessage();
    message.channel = channel;
    message.from = joiner;
    message.content =
        "{\"id\":\"%s\", \"username\":\"%s\"}"
            .formatted(joiner.getId().toString(), joiner.getUserName());
    message.messageType = MessageType.JOIN;
    return message;
  }

  public static GroupMessage leaveMessage(GroupChannel channel, User actor, User subject) {
    GroupMessage message = new GroupMessage();
    message.channel = channel;
    message.from = actor;
    message.content =
        "{\"id\":\"%s\", \"username\":\"%s\"}"
            .formatted(subject.getId().toString(), subject.getUserName());
    message.messageType = MessageType.LEAVE;
    return message;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof GroupChannel)) return false;
    GroupChannel that = (GroupChannel) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "PrivateMessage{"
        + "id="
        + id
        + ", channel="
        + channel
        + ", messageType="
        + messageType
        + ", from="
        + from
        + ", content='"
        + content
        + '\''
        + ", createAt="
        + createAt
        + ", updateAt="
        + updateAt
        + '}';
  }
}
