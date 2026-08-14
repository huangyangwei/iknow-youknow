package com.huangyangwei.iknow.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * JWT 工具（jjwt 0.12.x，HS256）：
 * 生成携带 userId/username/roles 的访问令牌，并校验解析。
 * 密钥长度须 ≥ 32 字节（256bit）。
 */
public class JwtUtil {

    private final SecretKey key;
    private final long expireSeconds;

    public JwtUtil(String secret, long expireSeconds) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireSeconds = expireSeconds;
    }

    public long getExpireSeconds() {
        return expireSeconds;
    }

    public String generate(Long userId, String username, List<String> roles) {
        return generate(userId, username, roles, List.of());
    }

    public String generate(Long userId, String username, List<String> roles, List<String> permissions) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("roles", roles)
                .claim("permissions", permissions)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expireSeconds)))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Long getUserId(String token) {
        return Long.valueOf(parse(token).getSubject());
    }

    public String getUsername(String token) {
        return parse(token).get("username", String.class);
    }

    @SuppressWarnings("unchecked")
    public List<String> getRoles(String token) {
        Object roles = parse(token).get("roles");
        return roles instanceof List ? (List<String>) roles : List.of();
    }

    @SuppressWarnings("unchecked")
    public List<String> getPermissions(String token) {
        Object permissions = parse(token).get("permissions");
        return permissions instanceof List ? (List<String>) permissions : List.of();
    }
}
