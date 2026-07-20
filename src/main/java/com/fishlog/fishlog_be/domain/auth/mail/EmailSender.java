package com.fishlog.fishlog_be.domain.auth.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/** 인증코드 메일 발송기. 비동기(@Async)로 처리해 발송 지연·실패가 가입 흐름을 막지 않게 한다. → docs/security.md §1-1 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailSender {

  private final JavaMailSender mailSender;

  @Value("${spring.mail.username}")
  private String from;

  @Async
  public void sendVerificationCode(String to, String code, long ttlSeconds) {
    try {
      SimpleMailMessage message = new SimpleMailMessage();
      message.setFrom(from);
      message.setTo(to);
      message.setSubject("[fishlog] 이메일 인증코드");
      message.setText(
          "fishlog 이메일 인증코드입니다.\n\n인증코드: "
              + code
              + "\n유효시간: "
              + (ttlSeconds / 60)
              + "분\n\n본인이 요청하지 않았다면 무시하세요.");
      mailSender.send(message);
      log.info("[email] 인증코드 발송 완료: {}", to);
    } catch (Exception e) {
      // 비동기라 예외를 삼키고 로깅만 — 사용자는 재전송으로 복구.
      log.warn("[email] 인증코드 발송 실패: {} ({})", to, e.getMessage());
    }
  }
}
