package com.example.Bookstore.security.jwt; // JWT 토큰에 담길 핵심 사용자 정보를 표현하는 패키지

import java.util.Collection; // 권한 목록을 표현하기 위해 Collection 사용
import java.util.List; // 불변 리스트 생성을 위해 List 사용
import java.util.Objects; // 생성자에서 null 검증을 간결하게 처리하기 위해 사용

/**
 * JWT 에 실어 보낼 최소한의 사용자 컨텍스트를 담는 값 객체입니다.
 * User 엔티티 전체를 노출하지 않고 필요한 정보만 전달하도록 분리해 두었습니다.
 */
public record JwtPrincipal(
    Long userId,
    String email,
    String displayName,
    String deviceId,
    Collection<String> authorities) {

  /**
   * 레코드 기본 생성자에서 필수 필드 검증과 불변 컬렉션 포장 작업을 수행합니다.
   */
  public JwtPrincipal {
    Objects.requireNonNull(userId, "userId 는 필수입니다"); // 사용자 식별자는 필수값
    Objects.requireNonNull(email, "email 은 필수입니다"); // 이메일도 JWT subject 로 활용 예정이라 필수값
    Objects.requireNonNull(authorities, "authorities 는 null 일 수 없습니다"); // 권한 컬렉션 필수
    authorities = List.copyOf(authorities); // 불변 리스트로 복사해 외부에서 수정하지 못하도록 합니다
  }
}
