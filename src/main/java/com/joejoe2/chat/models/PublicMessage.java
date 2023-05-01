package com.joejoe2.chat.models;

import java.time.Instant;
import java.util.Objects;
import javax.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(
    name = "public_message",
    indexes = {@Index(columnList = "channel_id"), @Index(columnList = "updateAt DESC")})
public class PublicMessage extends TimeStampBase {
  @Version
  @Column(nullable = false, columnDefinition = "TIMESTAMP DEFAULT now()")
  private Instant version;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @OnDelete(action = OnDeleteAction.CASCADE)
  private PublicChannel channel;

  @Column(length = 32, nullable = false)
  @Enumerated(EnumType.STRING)
  private MessageType messageType = MessageType.MESSAGE; // code level default

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  private User from;

  @Column(columnDefinition = "TEXT", nullable = false)
  private String content;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PublicMessage)) return false;
    PublicMessage that = (PublicMessage) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "PublicMessage{"
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
