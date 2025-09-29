package com.example.Bookstore.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * application.yml(app.password-reset.*)에 정의되는 비밀번호 재설정 관련 커스텀 설정 값입니다.
 */
@Component
@ConfigurationProperties(prefix = "app.password-reset")
public class PasswordResetProperties {

  /**
   * 비밀번호 재설정 화면의 기본 URL 입니다. 메일에 토큰을 붙인 링크를 만들 때 사용합니다.
   */
  private String baseUrl = "http://localhost:8080/password/reset";

  /**
   * 토큰이 발급된 후 얼마 동안 유효한지를 나타냅니다.
   */
  private Duration tokenExpiry = Duration.ofMinutes(30);

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public Duration getTokenExpiry() {
    return tokenExpiry;
  }

  public void setTokenExpiry(Duration tokenExpiry) {
    this.tokenExpiry = tokenExpiry;
  }
}
