package com.fishlog.fishlog_be.global.security;

import com.fishlog.fishlog_be.global.image.FishImageService;
import com.fishlog.fishlog_be.global.jwt.JwtAuthenticationFilter;
import com.fishlog.fishlog_be.global.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 필터 체인. 무상태(JWT) REST API. → docs/security.md §3
 *
 * <p>공개: 인증 API·조회성 GET(스팟/어종/랭킹/배너/관광/전체 도감)·도감 이미지 정적 파일·Swagger. 그 외는 인증 필요. CORS는 {@code
 * CorsConfig}의 소스를 사용.
 *
 * <p>랭킹은 목록 자체는 공개지만, 토큰이 있으면 필터가 principal(userId)을 세팅하므로 컨트롤러에서 로그인 시에만 내 순위(me)를 채운다. 스팟 목록의 찜
 * 여부(isFavorite)와 도감의 획득 여부(caught)도 같은 "공개지만 토큰을 보면 더 준다" 패턴이다.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtProvider jwtProvider;
  private final JwtAuthenticationEntryPoint authenticationEntryPoint;
  private final JwtAccessDeniedHandler accessDeniedHandler;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.cors(Customizer.withDefaults())
        .csrf(csrf -> csrf.disable())
        .httpBasic(basic -> basic.disable())
        .formLogin(form -> form.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/api/auth/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/spots",
                        "/api/spots/*",
                        "/api/fish/**",
                        "/api/rankings/**",
                        "/api/banner/**",
                        "/api/tours/**",
                        // 전체 도감 그리드. 비로그인 둘러보기에서도 보여야 하므로 공개(선택적 인증) —
                        // 토큰이 있으면 잡은 칸이 어종 이미지로, 없으면 전부 그림자로 내려간다.
                        // 칸을 눌렀을 때의 상세(GET /api/collections)는 아래 authenticated() 로 남는다.
                        "/api/collections/dex",
                        // 도감 이미지 정적 파일(어종/그림자/기본). 로그인 전 화면·<img> 태그에서 받아야 하므로 공개.
                        FishImageService.URL_PREFIX + "**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            ex ->
                ex.authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler))
        .addFilterBefore(
            new JwtAuthenticationFilter(jwtProvider), UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
      throws Exception {
    return configuration.getAuthenticationManager();
  }
}
