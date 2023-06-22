package com.joejoe2.chat.config;

import java.security.interfaces.RSAPublicKey;

import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class JwtConfig {
  @Value("${jwt.secret.publicKey}")
  private RSAPublicKey publicKey;

  @Bean
  public JwtParser parser(){
    return Jwts.parserBuilder().setSigningKey(publicKey).build();
  }
}
