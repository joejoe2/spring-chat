package com.joejoe2.chat.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.security.interfaces.RSAPublicKey;

@Data
@Configuration
public class JwtConfig {
    @Value("${jwt.secret.publicKey}")
    private RSAPublicKey publicKey;
}
