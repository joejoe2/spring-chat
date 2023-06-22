package com.joejoe2.chat.service.jwt;

import com.joejoe2.chat.config.JwtConfig;
import com.joejoe2.chat.data.UserDetail;
import com.joejoe2.chat.exception.InvalidTokenException;
import com.joejoe2.chat.service.redis.RedisService;
import com.joejoe2.chat.utils.JwtUtil;
import io.jsonwebtoken.JwtParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class JwtServiceImpl implements JwtService {
  @Autowired JwtConfig jwtConfig;
  @Autowired RedisService redisService;
  @Autowired
  JwtParser jwtParser;

  private static final Logger logger = LoggerFactory.getLogger(JwtServiceImpl.class);

  @Override
  public UserDetail getUserDetailFromAccessToken(String token) throws InvalidTokenException {
    return JwtUtil.extractUserDetailFromAccessToken(jwtParser, token);
  }

  @Override
  public boolean isAccessTokenInBlackList(String accessPlainToken) {
    return redisService.has("revoked_access_token:{" + accessPlainToken + "}");
  }
}
