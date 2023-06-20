package com.joejoe2.chat.repository.channel;

import com.joejoe2.chat.models.PrivateChannel;
import com.joejoe2.chat.models.User;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PrivateChannelRepository extends JpaRepository<PrivateChannel, UUID> {
  Optional<PrivateChannel> findById(UUID id);

  Optional<PrivateChannel> findByUniqueUserIds(String id);

  @Query(
      "SELECT DISTINCT ch from User u "
          + "join u.privateChannels ch where u = :user "
          + "ORDER BY ch.updateAt DESC")
  List<PrivateChannel> findByMembersContainingUserByUpdateAtDesc(@Param("user") User user);

  default List<PrivateChannel> findByIsUserInMembers(User user) {
    return findByMembersContainingUserByUpdateAtDesc(user);
  }

  @Query(
      "SELECT DISTINCT ch from User u "
          + "join u.privateChannels ch where u = :user "
          + "ORDER BY ch.updateAt DESC")
  Slice<PrivateChannel> findByMembersContainingUserByUpdateAtDesc(
      @Param("user") User user, Pageable pageable);

  default Slice<PrivateChannel> findByIsUserInMembers(User user, Pageable pageable) {
    return findByMembersContainingUserByUpdateAtDesc(user, pageable);
  }

  default boolean isPrivateChannelExistBetween(User user1, User user2) {
    UUID[] ids = new UUID[] {user1.getId(), user2.getId()};
    Arrays.sort(ids);
    return findByUniqueUserIds(ids[0].toString() + ids[1].toString()).isPresent();
  }

  @Cacheable(
      value = "PrivateChannelMembers",
      key = "'PrivateChannelMembers:{'+ #id.toString() +'}'")
  @Query("SELECT u.id from PrivateChannel ch join ch.members u where ch.id = :id")
  List<UUID> getMembersIdByChannel(@Param("id") UUID id);
}
