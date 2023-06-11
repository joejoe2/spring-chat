package com.joejoe2.chat.repository.message;

import com.joejoe2.chat.models.GroupChannel;
import com.joejoe2.chat.models.GroupMessage;
import com.joejoe2.chat.models.User;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupMessageRepository extends JpaRepository<GroupMessage, UUID> {
  Optional<GroupMessage> findById(UUID id);

  @Query(
      "SELECT msg FROM GroupMessage msg WHERE msg.channel = :channel AND msg.updateAt >= :since"
          + " ORDER BY msg.updateAt DESC")
  Slice<GroupMessage> findAllByChannelSince(
      @Param("channel") GroupChannel channel, @Param("since") Instant since, Pageable pageable);

  @Query("SELECT msg FROM GroupMessage msg WHERE msg.channel = :channel ORDER BY msg.updateAt DESC")
  Slice<GroupMessage> findAllByChannel(@Param("channel") GroupChannel channel, Pageable pageable);

  @Query(
      "SELECT invitation.invitationMessage from GroupInvitation invitation "
          + "where invitation.user = :user "
          + "and invitation.createAt >= :since ORDER BY invitation.createAt DESC")
  Slice<GroupMessage> findInvitations(
      @Param("user") User user, @Param("since") Instant since, Pageable pageable);

  void deleteByCreateAtLessThan(Instant dateTime);
}
