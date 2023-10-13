package com.joejoe2.chat.config;

import com.joejoe2.chat.interceptor.ControllerConstraintInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebMvc
public class InterceptorConfig implements WebMvcConfigurer {
  private final ControllerConstraintInterceptor controllerConstraintInterceptor;

  public InterceptorConfig(ControllerConstraintInterceptor controllerConstraintInterceptor) {
    this.controllerConstraintInterceptor = controllerConstraintInterceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(controllerConstraintInterceptor).addPathPatterns("/**");
  }
}
