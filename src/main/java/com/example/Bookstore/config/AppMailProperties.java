package com.example.Bookstore.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 커스텀 메일 관련 설정(app.mail.*)을 바인딩하는 전용 프로퍼티 클래스입니다.
 */
@Component
@ConfigurationProperties(prefix = "app.mail")
public class AppMailProperties {

  /**
   * 송신자 주소. 별도 설정이 없으면 기본값(no-reply@bookstore.local)을 사용합니다.
   */
  private String from = "no-reply@bookstore.local";

  public String getFrom() {
    return from;
  }

  public void setFrom(String from) {
    this.from = from;
  }
}
