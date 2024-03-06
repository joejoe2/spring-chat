package com.joejoe2.chat.models;

import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@EqualsAndHashCode(callSuper = true)
@MappedSuperclass
@Data
public class TimeStampBase extends Base {
  @CreationTimestamp Instant createAt;

  @UpdateTimestamp Instant updateAt;
}
