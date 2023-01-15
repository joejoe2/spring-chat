package com.joejoe2.chat.repository.message;

import com.joejoe2.chat.models.PrivateChannel;
import com.joejoe2.chat.models.PrivateMessage;
import com.joejoe2.chat.models.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PrivateMessageRepository extends JpaRepository<PrivateMessage, UUID> {
    Optional<PrivateMessage> findById(UUID id);

    @Query(nativeQuery = true, value = "SELECT * FROM private_message WHERE channel_id = :channel " +
            "AND update_at >= :since ORDER BY update_at DESC")
    Slice<PrivateMessage> findAllByChannelSince(@Param("channel") PrivateChannel channel, @Param("since") Instant since, Pageable pageable);

    @Query(nativeQuery = true,
            value = "SELECT * FROM private_message WHERE channel_id = :channel " +
                    "ORDER BY update_at DESC")
    Slice<PrivateMessage> findAllByChannel(@Param("channel") PrivateChannel channel, Pageable pageable);

    @Query(nativeQuery = true,
            value = "SELECT * FROM private_message WHERE to_id = :user OR from_id = :user " +
                    "AND update_at >= :since ORDER BY update_at DESC")
    Slice<PrivateMessage> findAllByUserSince(@Param("user") User user, @Param("since") Instant since, Pageable pageable);

    @Query(nativeQuery = true,
            value = "SELECT * FROM private_message WHERE to_id = :user OR from_id = :user " +
                    "ORDER BY update_at DESC")
    Slice<PrivateMessage> findAllByUser(@Param("user") User user, Pageable pageable);

    void deleteByCreateAtLessThan(Instant dateTime);
}
