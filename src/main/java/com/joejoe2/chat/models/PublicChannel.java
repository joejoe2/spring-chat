package com.joejoe2.chat.models;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import javax.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

@Data
@NoArgsConstructor
@Entity
@BatchSize(size = 128)
@Table(name = "public_channel")
public class PublicChannel extends TimeStampBase {
  @Version
  @Column(nullable = false, columnDefinition = "TIMESTAMP DEFAULT now()")
  private Instant version;

  @Column(unique = true, updatable = false, nullable = false, length = 128)
  String name;

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "channel", orphanRemoval = true)
  @BatchSize(size = 128)
  List<PublicMessage> messages;

  public PublicChannel(String name) {
    this.name = name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PublicChannel)) return false;
    PublicChannel channel = (PublicChannel) o;
    return Objects.equals(id, channel.id) && Objects.equals(name, channel.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name);
  }

  @Override
  public String toString() {
    return "PublicChannel{"
        + "id="
        + id
        + ", name='"
        + name
        + '\''
        + ", createAt="
        + createAt
        + ", updateAt="
        + updateAt
        + '}';
  }
}
