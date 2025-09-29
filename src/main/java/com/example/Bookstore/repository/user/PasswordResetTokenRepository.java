package com.example.Bookstore.repository.user;

import com.example.Bookstore.domain.user.PasswordResetToken;
import com.example.Bookstore.domain.user.User;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

/**
 * 비밀번호 재설정 토큰에 대한 CRUD 를 담당하는 Spring Data JPA 리포지토리입니다.
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

  /** 토큰 문자열로 단건 조회합니다. */
  Optional<PasswordResetToken> findByToken(String token);

  /** 특정 사용자의 토큰을 모두 삭제합니다. 새 토큰 발급 시 중복 사용을 막기 위해 호출합니다. */
  void deleteByUser(User user);

  /** 만료 시각이 threshold 이전인 토큰을 일괄 삭제합니다. */
  @Modifying
  void deleteAllByExpiresAtBefore(LocalDateTime threshold);
}
