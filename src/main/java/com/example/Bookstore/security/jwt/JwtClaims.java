package com.example.Bookstore.security.jwt; // JWT 파싱 결과를 표현하는 패키지

import java.time.Instant; // iat/exp 같은 시간 클레임을 표현
import java.util.Collection; // 권한·스코프 목록 표현
import java.util.List; // 불변 컬렉션 변환
import java.util.Objects; // null 검증

/**
 * JWT 에서 추출한 표준/커스텀 클레임을 한데 모아 전달하는 값 객체입니다.
 * Access/Refresh 토큰 모두 동일한 구조에서 필요한 값만 사용하도록 설계했습니다.
 */
public record JwtClaims(
    String tokenId,
    Long userId,
    String email,
    String displayName,
    String issuer,
    Instant issuedAt,
    Instant expiresAt,
    String audience,
    Collection<String> authorities,
    String deviceId) {

  /**
   * 불변성과 필수 값 검증을 담당하는 컴팩트 생성자.
   */
  public JwtClaims {
    Objects.requireNonNull(tokenId, "tokenId 는 null 일 수 없습니다"); // JTI 는 회전·블랙리스트에서 핵심 키
    Objects.requireNonNull(userId, "userId 는 null 일 수 없습니다"); // 사용자 식별자
    Objects.requireNonNull(email, "email 는 null 일 수 없습니다"); // 이메일 필수
    Objects.requireNonNull(issuedAt, "issuedAt 는 null 일 수 없습니다"); // 발급 시각
    Objects.requireNonNull(expiresAt, "expiresAt 는 null 일 수 없습니다"); // 만료 시각
    Objects.requireNonNull(authorities, "authorities 는 null 일 수 없습니다"); // 권한 리스트
    authorities = List.copyOf(authorities); // 외부에서 수정하지 못하도록 불변 리스트로 변환
  }
}
