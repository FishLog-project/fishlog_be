package com.fishlog.fishlog_be.global.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 요청의 {@code Authorization: Bearer} Access 토큰을 검증해 인증을 세팅한다. (stateless)
 *
 * <p>유효한 Access 토큰이면 userId를 principal로 하는 인증을 SecurityContext에 넣는다. 무효/부재 시 인증 없이 통과시키고, 보호 리소스
 * 접근은 SecurityConfig의 엔트리포인트(401)가 처리한다. → docs/security.md §2
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String HEADER = "Authorization";
  private static final String PREFIX = "Bearer ";

  private final JwtProvider jwtProvider;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String token = resolveToken(request);
    if (token != null && jwtProvider.validate(token) && jwtProvider.isAccessToken(token)) {
      Long userId = jwtProvider.getUserId(token);
      UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(userId, null, List.of());
      SecurityContextHolder.getContext().setAuthentication(authentication);
    }
    filterChain.doFilter(request, response);
  }

  private String resolveToken(HttpServletRequest request) {
    String header = request.getHeader(HEADER);
    if (StringUtils.hasText(header) && header.startsWith(PREFIX)) {
      return header.substring(PREFIX.length());
    }
    return null;
  }
}
