package com.joejoe2.chat.repository.channel;

import com.joejoe2.chat.models.GroupChannel;
import com.joejoe2.chat.models.User;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupChannelRepository extends JpaRepository<GroupChannel, UUID> {
  Optional<GroupChannel> findById(UUID id);

  @Query(
      "SELECT DISTINCT ch from User u "
          + "join u.groupChannels ch where u = :user "
          + "and ch.updateAt >= :since ORDER BY ch.updateAt DESC")
  List<GroupChannel> findByMembersContainingUserByUpdateAtDesc(
      @Param("user") User user, @Param("since") Instant since);

  default List<GroupChannel> findByIsUserInMembers(User user, Instant since) {
    return findByMembersContainingUserByUpdateAtDesc(user, since);
  }

  @Query(
      "SELECT DISTINCT ch from User u "
          + "join u.groupChannels ch where u = :user "
          + "and ch.updateAt >= :since ORDER BY ch.updateAt DESC")
  Slice<GroupChannel> findByMembersContainingUserByUpdateAtDesc(
      @Param("user") User user, @Param("since") Instant since, Pageable pageable);

  default Slice<GroupChannel> findByIsUserInMembers(User user, Instant since, Pageable pageable) {
    return findByMembersContainingUserByUpdateAtDesc(user, since, pageable);
  }

  /*@Query(
      "SELECT DISTINCT ch from User u "
          + "join u.invitedChannels ch where u = :user "
          + "and ch.updateAt >= :since ORDER BY ch.updateAt DESC")
  Slice<GroupChannel> findByPendingContainingUserByUpdateAtDesc(
      @Param("user") User user, @Param("since") Instant since, Pageable pageable);

  default Slice<GroupChannel> findByIsUserInvited(User user, Instant since, Pageable pageable) {
    return findByPendingContainingUserByUpdateAtDesc(user, since, pageable);
  }*/
}
