package com.joejoe2.chat.service.user.auth;

import com.joejoe2.chat.data.UserDetail;
import com.joejoe2.chat.models.User;
import com.joejoe2.chat.repository.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserDetailService implements UserDetailsService {
    @Autowired
    UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.getByUserName(username).orElseThrow(() -> new UsernameNotFoundException("user does not exist !"));
        return new UserDetail(user);
    }

    public void createUserIfAbsent(UserDetail userDetail) {
        if (!userRepository.existsById(UUID.fromString(userDetail.getId()))) {
            User user = User.builder()
                    .id(UUID.fromString(userDetail.getId()))
                    .userName(userDetail.getUsername())
                    .build();
            userRepository.save(user);
        }
    }
}
