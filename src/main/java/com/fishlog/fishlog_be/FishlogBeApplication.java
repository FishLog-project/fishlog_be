package com.fishlog.fishlog_be;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing // 생성/수정 이벤트를 감지하여 값을 자동으로 주입
@SpringBootApplication
public class FishlogBeApplication {

  public static void main(String[] args) {
    SpringApplication.run(FishlogBeApplication.class, args);
  }
}
