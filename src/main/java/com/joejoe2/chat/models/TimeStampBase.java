package com.joejoe2.chat.models;

import java.time.Instant;
import javax.persistence.MappedSuperclass;
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
