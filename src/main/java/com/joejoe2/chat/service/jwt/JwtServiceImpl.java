package com.joejoe2.chat.service.jwt;

import com.joejoe2.chat.config.JwtConfig;
import com.joejoe2.chat.data.UserDetail;
import com.joejoe2.chat.exception.InvalidTokenException;
import com.joejoe2.chat.repository.user.UserRepository;
import com.joejoe2.chat.utils.JwtUtil;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class JwtServiceImpl implements JwtService{
    @Autowired
    private JwtConfig jwtConfig;
    @Autowired
    UserRepository userRepository;

    private static final Logger logger = LoggerFactory.getLogger(JwtServiceImpl.class);

    @Override
    public UserDetail getUserDetailFromAccessToken(String token) throws InvalidTokenException {
        return JwtUtil.extractUserDetailFromAccessToken(jwtConfig.getPublicKey(), token);
    }
}
