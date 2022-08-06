package com.joejoe2.chat.utils;

import com.joejoe2.chat.data.UserDetail;
import com.joejoe2.chat.exception.InvalidTokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;

import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class JwtUtil {
    public static Map<String, Object> parseToken(RSAPublicKey key, String token) throws JwtException {
        try {
            JwtParser parser = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build();

            Claims claims = parser
                    .parseClaimsJws(token)
                    .getBody();

            return claims.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }catch (Exception e){
            throw new JwtException(e.getMessage());
        }
    }

    private static final String[] REQUIRED_FIELDS = new String[]{
            "type", "id", "username"
    };

    public static UserDetail extractUserDetailFromAccessToken(RSAPublicKey publicKey, String token) throws InvalidTokenException {
        try {
            Map<String, Object> data = JwtUtil.parseToken(publicKey, token);
            if (Arrays.stream(REQUIRED_FIELDS).anyMatch((f)-> data.get(f)==null)){
                throw new InvalidTokenException("invalid token !");
            }
            if (!data.get("type").equals("access_token")){
                throw new InvalidTokenException("invalid token !");
            }
            return new UserDetail((String) data.get("id"), (String) data.get("username"));
        }catch (Exception ex){
            throw new InvalidTokenException("invalid token !");
        }
    }
}
