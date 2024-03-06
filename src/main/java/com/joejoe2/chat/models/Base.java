package com.joejoe2.chat.models;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.util.UUID;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

@MappedSuperclass
@Data
public class Base {
  @Id
  @GeneratedValue(generator = "UUIDv7")
  @GenericGenerator(name = "UUIDv7", strategy = "com.joejoe2.chat.models.UUIDv7Generator")
  @Column(unique = true, updatable = false, nullable = false)
  UUID id;
}
