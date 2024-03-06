package com.joejoe2.chat.filter;

import com.joejoe2.chat.data.UserDetail;
import com.joejoe2.chat.exception.InvalidTokenException;
import com.joejoe2.chat.service.jwt.JwtService;
import com.joejoe2.chat.service.user.UserService;
import com.joejoe2.chat.utils.AuthUtil;
import com.joejoe2.chat.utils.HttpUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
  private final JwtService jwtService;
  private final UserService userService;

  public JwtAuthenticationFilter(JwtService jwtService, UserService userService) {
    this.jwtService = jwtService;
    this.userService = userService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    try {
      String accessToken = HttpUtil.extractAccessToken(request);
      if (accessToken != null) {
        if (jwtService.isAccessTokenInBlackList(accessToken))
          throw new InvalidTokenException("access token has been revoked !");
        UserDetail userDetail = jwtService.getUserDetailFromAccessToken(accessToken);
        userService.createUserIfAbsent(userDetail);
        AuthUtil.setCurrentUserDetail(userDetail);
      }
    } catch (InvalidTokenException e) {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      return;
    }
    filterChain.doFilter(request, response);
  }

  @Override
  protected boolean shouldNotFilterAsyncDispatch() {
    return false;
  }
}
