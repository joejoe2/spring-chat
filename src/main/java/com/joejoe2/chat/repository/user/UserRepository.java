package com.joejoe2.chat.repository.user;

import com.joejoe2.chat.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findById(UUID id);
    Optional<User> getByUserName(String username);
}
