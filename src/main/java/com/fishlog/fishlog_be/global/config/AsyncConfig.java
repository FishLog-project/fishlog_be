package com.fishlog.fishlog_be.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** 비동기 실행 활성화(@Async). 인증코드 메일 발송 등에 사용. → docs/security.md §1-1 */
@Configuration
@EnableAsync
public class AsyncConfig {}
