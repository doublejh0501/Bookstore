package com.example.Bookstore.security.jwt; // JWT 리프레시 토큰 저장 책임을 정의하는 패키지

import java.time.Instant; // 만료 시각 표현에 사용
import java.util.Objects; // 레코드에서 null 검증에 사용

/**
 * 리프레시 토큰 화이트리스트(예: Redis)를 추상화한 서비스 인터페이스입니다.
 * 실제 구현은 RedisTemplate 등을 사용해 저장/회전/재사용 감지를 수행하게 됩니다.
 */
public interface RefreshTokenService {

  /**
   * 최초 로그인 시 새 리프레시 토큰을 저장하도록 호출되는 명령입니다.
   */
  void store(StoreCommand command);

  /**
   * 리프레시 토큰 재발급(회전) 시 기존 토큰을 검증하고 새 토큰으로 교체하는 명령입니다.
   */
  RotationStatus rotate(RotationCommand command);

  /**
   * 특정 JTI 를 즉시 무효화합니다. 로그아웃 혹은 재사용 감지 후 처리를 위해 사용됩니다.
   */
  void invalidate(String jti);

  /**
   * 재사용 감지 시 계정/기기 단위의 모든 리프레시 토큰을 무효화할 때 사용합니다.
   */
  void invalidateFamily(String subject, String deviceId);

  /**
   * 리프레시 토큰 신규 저장에 필요한 정보를 축약해 전달합니다.
   */
  record StoreCommand(String jti, String subject, String deviceId, Instant expiresAt, String hashedToken) {
    public StoreCommand {
      Objects.requireNonNull(jti, "jti 는 null 일 수 없습니다");
      Objects.requireNonNull(subject, "subject 는 null 일 수 없습니다");
      Objects.requireNonNull(expiresAt, "expiresAt 는 null 일 수 없습니다");
      Objects.requireNonNull(hashedToken, "hashedToken 은 null 일 수 없습니다");
    }
  }

  /**
   * 회전 시 필요한 입력 값을 모아둔 명령 객체입니다.
   * presentedHashedToken 은 클라이언트가 제시한 리프레시 토큰의 해시로 재사용 감지에 활용합니다.
   */
  record RotationCommand(
      String previousJti,
      String newJti,
      String subject,
      String deviceId,
      Instant newExpiresAt,
      String presentedHashedToken,
      String newHashedToken) {
    public RotationCommand {
      Objects.requireNonNull(previousJti, "previousJti 는 null 일 수 없습니다");
      Objects.requireNonNull(newJti, "newJti 는 null 일 수 없습니다");
      Objects.requireNonNull(subject, "subject 는 null 일 수 없습니다");
      Objects.requireNonNull(newExpiresAt, "newExpiresAt 는 null 일 수 없습니다");
      Objects.requireNonNull(presentedHashedToken, "presentedHashedToken 은 null 일 수 없습니다");
      Objects.requireNonNull(newHashedToken, "newHashedToken 는 null 일 수 없습니다");
    }
  }

  /**
   * 회전 결과: 재사용 감지 여부와 추가 조치를 호출 측에 알려줍니다.
   * reuseDetected 가 true 인 경우 reusedJti 는 재사용된 토큰의 식별자를 담습니다.
   */
  record RotationStatus(boolean reuseDetected, String reusedJti) {
  }
}
