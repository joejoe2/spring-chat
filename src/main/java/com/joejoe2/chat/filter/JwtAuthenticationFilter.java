package com.joejoe2.chat.filter;

import com.joejoe2.chat.data.UserDetail;
import com.joejoe2.chat.exception.InvalidTokenException;
import com.joejoe2.chat.service.jwt.JwtService;
import com.joejoe2.chat.service.user.auth.UserDetailService;
import com.joejoe2.chat.utils.AuthUtil;
import com.joejoe2.chat.utils.HttpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Autowired
    JwtService jwtService;
    @Autowired
    UserDetailService userDetailService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            String accessToken = HttpUtil.extractAccessToken(request);
            if (accessToken != null){
                UserDetail userDetail = jwtService.getUserDetailFromAccessToken(accessToken);
                userDetailService.createUserIfAbsent(userDetail);
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
