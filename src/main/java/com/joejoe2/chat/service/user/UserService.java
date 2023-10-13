package com.joejoe2.chat.service.user;

import com.joejoe2.chat.data.UserDetail;
import com.joejoe2.chat.exception.UserDoesNotExist;
import com.joejoe2.chat.models.User;
import com.joejoe2.chat.repository.user.UserRepository;
import com.joejoe2.chat.validation.validator.UUIDValidator;
import java.util.UUID;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService implements UserDetailsService {
  private final UserRepository userRepository;
  private final UUIDValidator uuidValidator = UUIDValidator.getInstance();

  public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Transactional
  public User getUserById(String userId) throws UserDoesNotExist {
    return userRepository
        .findById(uuidValidator.validate(userId))
        .orElseThrow(
            () -> new UserDoesNotExist("user with id=%s does not exist !".formatted(userId)));
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    User user =
        userRepository
            .getByUserName(username)
            .orElseThrow(() -> new UsernameNotFoundException("user does not exist !"));
    return new UserDetail(user);
  }

  public void createUserIfAbsent(UserDetail userDetail) {
    if (!userRepository.existsById(UUID.fromString(userDetail.getId()))) {
      User user =
          User.builder()
              .id(UUID.fromString(userDetail.getId()))
              .userName(userDetail.getUsername())
              .build();
      userRepository.save(user);
    }
  }
}
