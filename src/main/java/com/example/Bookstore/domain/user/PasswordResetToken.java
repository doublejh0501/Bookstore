package com.example.Bookstore.domain.user;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 비밀번호 재설정을 위한 1회용 토큰 엔티티입니다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "password_reset_tokens", uniqueConstraints = {
    @UniqueConstraint(name = "uk_password_reset_token", columnNames = {"token"})
})
public class PasswordResetToken {

  /** 내부적으로 식별에 사용하는 PK 입니다. */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** 토큰이 연결된 실제 사용자 계정입니다. (N:1 관계) */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_password_reset_token_user"))
  private User user;

  /** 메일에 담기는 토큰 문자열입니다. 유니크 제약조건으로 중복을 방지합니다. */
  @Column(nullable = false, length = 120)
  private String token;

  /** 이 토큰이 더 이상 사용 불가능해지는 시각입니다. */
  @Column(nullable = false)
  private LocalDateTime expiresAt;

  /** 토큰이 발급된 시각입니다. */
  @Column(nullable = false)
  private LocalDateTime createdAt;

  /** 사용 시각. 한 번이라도 쓰였다면 {@link #isUsed()}가 true 를 반환합니다. */
  private LocalDateTime usedAt;

  /** 현재 시각 기준 만료 여부를 판단합니다. */
  public boolean isExpired(LocalDateTime currentTime) {
    return expiresAt.isBefore(currentTime);
  }

  /** 사용 여부를 단순히 usedAt 필드 존재로 확인합니다. */
  public boolean isUsed() {
    return usedAt != null;
  }

  /** 토큰 사용 시 호출하여 사용 시각을 기록합니다. */
  public void markUsed(LocalDateTime currentTime) {
    this.usedAt = currentTime;
  }
}
