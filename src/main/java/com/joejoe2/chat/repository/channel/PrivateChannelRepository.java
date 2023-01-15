package com.joejoe2.chat.repository.channel;


import com.joejoe2.chat.models.PrivateChannel;
import com.joejoe2.chat.models.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PrivateChannelRepository extends JpaRepository<PrivateChannel, UUID> {
    Optional<PrivateChannel> findById(UUID id);

    Optional<PrivateChannel> findByUniqueUserIds(String id);

    @Query("SELECT DISTINCT ch from PrivateChannel ch " +
            "join fetch ch.members WHERE :user MEMBER ch.members " +
            "ORDER BY ch.updateAt DESC")
    List<PrivateChannel> findByMembersContainingUserByUpdateAtDesc(@Param("user") User user);

    default List<PrivateChannel> findByIsUserInMembers(User user) {
        return findByMembersContainingUserByUpdateAtDesc(user);
    }

    @Query("SELECT DISTINCT ch from PrivateChannel ch " +
            "join fetch ch.members WHERE :user MEMBER ch.members " +
            "ORDER BY ch.updateAt DESC")
    Slice<PrivateChannel> findByMembersContainingUserByUpdateAtDesc(@Param("user") User user, Pageable pageable);

    default Slice<PrivateChannel> findByIsUserInMembers(User user, Pageable pageable) {
        return findByMembersContainingUserByUpdateAtDesc(user, pageable);
    }

    default boolean isPrivateChannelExistBetween(User user1, User user2) {
        String[] ids = new String[]{user1.getId().toString(), user2.getId().toString()};
        Arrays.sort(ids);
        return findByUniqueUserIds(ids[0] + ids[1]).isPresent();
    }
}
