package com.joejoe2.chat.service.jwt;

import com.joejoe2.chat.data.UserDetail;
import com.joejoe2.chat.exception.InvalidTokenException;

public interface JwtService {
  /**
   * retrieve UserDetail from access token
   *
   * @param token access token in plaintext
   * @return related UserDetail with the access token
   * @throws InvalidTokenException if the access token is invalid
   */
  UserDetail getUserDetailFromAccessToken(String token) throws InvalidTokenException;

  boolean isAccessTokenInBlackList(String accessPlainToken);
}
