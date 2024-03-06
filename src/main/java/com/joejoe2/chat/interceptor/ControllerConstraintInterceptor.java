package com.joejoe2.chat.interceptor;

import com.joejoe2.chat.controller.constraint.checker.ControllerAuthConstraintChecker;
import com.joejoe2.chat.exception.ControllerConstraintViolation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ControllerConstraintInterceptor implements HandlerInterceptor {
  private final ControllerAuthConstraintChecker authConstraintChecker;

  private static final Logger logger =
      LoggerFactory.getLogger(ControllerConstraintInterceptor.class);

  public ControllerConstraintInterceptor(ControllerAuthConstraintChecker authConstraintChecker) {
    this.authConstraintChecker = authConstraintChecker;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    try {
      if (handler instanceof HandlerMethod) {
        authConstraintChecker.checkWithMethod(((HandlerMethod) handler).getMethod());
      }
    } catch (ControllerConstraintViolation ex) {
      setJsonResponse(response, ex.getRejectStatus(), ex.getRejectMessage());
      return false;
    } catch (Exception e) {
      logger.error(e.getMessage());
      setJsonResponse(response, 500, "");
      return false;
    }

    return true;
  }

  private void setJsonResponse(HttpServletResponse response, int status, String message) {
    if (message != null && !message.isEmpty()) {
      try {
        response.getWriter().write("{ \"message\": \"" + message + "\"}");
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    response.setStatus(status);
  }
}
