package com.fishlog.fishlog_be.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc(OpenAPI) 설정. Swagger UI: {@code /swagger-ui.html}.
 *
 * <p>보호(인증) 엔드포인트는 {@code @Operation(security = @SecurityRequirement(name = "JWT"))}로 표기하며, 그 이름은
 * 여기 등록하는 Bearer JWT SecurityScheme 이름({@link #SECURITY_SCHEME_NAME})과 일치해야 Swagger UI의 Authorize에서
 * 토큰을 넣을 수 있다. 전역 SecurityRequirement는 걸지 않는다(공개 엔드포인트에 자물쇠가 붙지 않도록, 보호 엔드포인트만 개별 선언). →
 * docs/architecture.md "컨트롤러 Swagger 문서화 규칙", docs/security.md
 */
@Configuration
public class SwaggerConfig {

  /** 보호 엔드포인트의 {@code @SecurityRequirement(name = "JWT")}와 일치해야 하는 스킴 이름. */
  public static final String SECURITY_SCHEME_NAME = "JWT";

  @Bean
  public OpenAPI openAPI() {
    return new OpenAPI()
        .info(apiInfo())
        .components(new Components().addSecuritySchemes(SECURITY_SCHEME_NAME, jwtSecurityScheme()));
  }

  private Info apiInfo() {
    return new Info().title("Fishlog API").description("Fishlog 백엔드 API 문서").version("v1");
  }

  /** Authorization: Bearer {accessToken} 형식의 JWT 인증 스킴. */
  private SecurityScheme jwtSecurityScheme() {
    return new SecurityScheme()
        .name(SECURITY_SCHEME_NAME)
        .type(SecurityScheme.Type.HTTP)
        .scheme("bearer")
        .bearerFormat("JWT");
  }
}
