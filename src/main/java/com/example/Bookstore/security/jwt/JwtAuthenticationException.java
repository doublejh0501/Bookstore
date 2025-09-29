package com.example.Bookstore.security.jwt; // JWT 인증 과정에서 발생하는 예외 패키지

/**
 * JWT 생성/검증 단계에서 발생하는 비즈니스 예외를 명확하게 표현하기 위한 전용 예외입니다.
 * 나중에 Spring Security 예외 변환 필터에서 잡아서 401/403 응답으로 매핑하게 됩니다.
 */
public class JwtAuthenticationException extends RuntimeException {

  /**
   * 상세 메시지만 전달할 때 사용하는 생성자.
   */
  public JwtAuthenticationException(String message) {
    super(message); // 상위 RuntimeException 에 메시지를 전달합니다
  }

  /**
   * 원인 예외까지 함께 감싸고 싶을 때 사용하는 생성자.
   */
  public JwtAuthenticationException(String message, Throwable cause) {
    super(message, cause); // cause 체인 유지
  }
}
