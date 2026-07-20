package com.fishlog.fishlog_be.global.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * JWT 발급·검증. HS256 서명(시크릿은 {@code jwt.secret}). Access/Refresh 두 종류를 구분자 클레임({@code type})으로 구별한다.
 * → docs/security.md §2
 *
 * <p>권한(role) 클레임은 role 도입 시 추가 예정.
 */
@Component
public class JwtProvider {

  private static final String CLAIM_TYPE = "type";
  private static final String TYPE_ACCESS = "access";
  private static final String TYPE_REFRESH = "refresh";

  private final SecretKey key;
  private final long accessTtlSeconds;
  private final long refreshTtlSeconds;

  public JwtProvider(
      @Value("${jwt.secret}") String secret,
      @Value("${jwt.access-ttl-seconds}") long accessTtlSeconds,
      @Value("${jwt.refresh-ttl-seconds}") long refreshTtlSeconds) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.accessTtlSeconds = accessTtlSeconds;
    this.refreshTtlSeconds = refreshTtlSeconds;
  }

  public String createAccessToken(Long userId) {
    return build(userId, TYPE_ACCESS, accessTtlSeconds);
  }

  public String createRefreshToken(Long userId) {
    return build(userId, TYPE_REFRESH, refreshTtlSeconds);
  }

  private String build(Long userId, String type, long ttlSeconds) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(String.valueOf(userId))
        .claim(CLAIM_TYPE, type)
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusSeconds(ttlSeconds)))
        .signWith(key)
        .compact();
  }

  /** 서명·만료가 유효하면 true. */
  public boolean validate(String token) {
    try {
      parse(token);
      return true;
    } catch (JwtException | IllegalArgumentException e) {
      return false;
    }
  }

  public Long getUserId(String token) {
    return Long.valueOf(parse(token).getSubject());
  }

  public boolean isAccessToken(String token) {
    return TYPE_ACCESS.equals(parse(token).get(CLAIM_TYPE, String.class));
  }

  public boolean isRefreshToken(String token) {
    return TYPE_REFRESH.equals(parse(token).get(CLAIM_TYPE, String.class));
  }

  public long getAccessTtlSeconds() {
    return accessTtlSeconds;
  }

  public long getRefreshTtlSeconds() {
    return refreshTtlSeconds;
  }

  private Claims parse(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }
}
