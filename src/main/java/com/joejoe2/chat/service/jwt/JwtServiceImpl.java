package com.joejoe2.chat.service.jwt;

import com.joejoe2.chat.config.JwtConfig;
import com.joejoe2.chat.data.UserDetail;
import com.joejoe2.chat.exception.InvalidTokenException;
import com.joejoe2.chat.service.redis.RedisService;
import com.joejoe2.chat.utils.JwtUtil;
import io.jsonwebtoken.JwtParser;
import org.springframework.stereotype.Service;

@Service
public class JwtServiceImpl implements JwtService {
  private final RedisService redisService;
  private final JwtParser jwtParser;

  public JwtServiceImpl(JwtConfig jwtConfig, RedisService redisService, JwtParser jwtParser) {
    this.redisService = redisService;
    this.jwtParser = jwtParser;
  }

  @Override
  public UserDetail getUserDetailFromAccessToken(String token) throws InvalidTokenException {
    return JwtUtil.extractUserDetailFromAccessToken(jwtParser, token);
  }

  @Override
  public boolean isAccessTokenInBlackList(String accessPlainToken) {
    return redisService.has("revoked_access_token:{" + accessPlainToken + "}");
  }
}
