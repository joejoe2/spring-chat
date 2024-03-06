package com.joejoe2.chat.repository.message;

import com.joejoe2.chat.models.PublicMessage;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PublicMessageRepository extends JpaRepository<PublicMessage, UUID> {
  Optional<PublicMessage> findById(UUID id);

  @Query(
      nativeQuery = true,
      value =
          "SELECT * FROM public_message WHERE channel_id = :channel "
              + "AND update_at >= :since ORDER BY update_at DESC")
  Slice<PublicMessage> findAllByChannelSince(
      @Param("channel") UUID channel, @Param("since") Instant since, Pageable pageable);

  @Query(
      nativeQuery = true,
      value =
          "SELECT * FROM public_message WHERE channel_id = :channel " + "ORDER BY update_at DESC")
  Slice<PublicMessage> findAllByChannel(@Param("channel") UUID channelId, Pageable pageable);

  void deleteByCreateAtLessThan(Instant dateTime);
}
