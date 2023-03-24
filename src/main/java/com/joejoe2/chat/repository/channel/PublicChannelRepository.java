package com.joejoe2.chat.repository.channel;

import com.joejoe2.chat.models.PublicChannel;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublicChannelRepository extends JpaRepository<PublicChannel, UUID> {
  Optional<PublicChannel> findById(UUID id);

  Optional<PublicChannel> findByName(String name);

  Page<PublicChannel> findAll(Pageable pageable);
}
