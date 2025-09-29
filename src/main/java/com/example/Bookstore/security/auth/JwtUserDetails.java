package com.example.Bookstore.security.auth; // JWT 인증 컨텍스트에서 사용할 UserDetails 구현

import com.example.Bookstore.domain.user.MemberStatus; // 계정 상태에 따라 enable 여부를 판단하기 위해 사용
import com.example.Bookstore.domain.user.Role; // 권한 정보를 Spring Security 에서 사용할 형태로 변환
import com.example.Bookstore.domain.user.User; // 도메인 사용자 엔티티
import com.example.Bookstore.security.jwt.JwtPrincipal; // JWT 발급 시 사용할 최소 사용자 정보
import java.util.Collection; // 권한 목록 표현
import java.util.List; // 불변 리스트 변환
import java.util.Objects; // null 검증 유틸
import java.util.stream.Collectors; // 권한 문자열 변환에 사용
import org.springframework.security.core.GrantedAuthority; // Spring Security 권한 표현
import org.springframework.security.core.authority.SimpleGrantedAuthority; // 문자열 권한 → GrantedAuthority 변환
import org.springframework.security.core.userdetails.UserDetails; // UserDetails 계약

/**
 * User 엔티티를 기반으로 JWT 토큰 생성/검증에 적합한 형태로 감싸는 UserDetails 구현체입니다.
 * <p>
 * - email 을 username 으로 사용합니다.
 * - MemberStatus 가 WITHDRAWN/INACTIVE 인 경우 계정을 비활성화합니다.
 */
public class JwtUserDetails implements UserDetails {

  private final Long userId; // 도메인 사용자 식별자
  private final String email; // 로그인 아이디로 사용하는 이메일
  private final String password; // 암호화된 비밀번호
  private final String displayName; // 토큰 클레임에 포함할 표시 이름
  private final MemberStatus status; // 계정 상태 (활성/비활성 판단 용도)
  private final Collection<? extends GrantedAuthority> authorities; // Spring Security 권한 컬렉션

  private JwtUserDetails(
      Long userId,
      String email,
      String password,
      String displayName,
      MemberStatus status,
      Collection<? extends GrantedAuthority> authorities) {
    this.userId = Objects.requireNonNull(userId, "userId 는 null 일 수 없습니다");
    this.email = Objects.requireNonNull(email, "email 은 null 일 수 없습니다");
    this.password = Objects.requireNonNull(password, "password 는 null 일 수 없습니다");
    this.displayName = displayName; // 표시 이름은 null 허용 (미등록 사용자 대비)
    this.status = status; // 상태 정보 저장 (null → 기본 ACTIVE 처리)
    this.authorities = List.copyOf(authorities); // 외부 수정 불가하도록 불변화
  }

  /**
   * User 엔티티에서 JwtUserDetails 로 변환합니다.
   */
  public static JwtUserDetails from(User user) {
    Objects.requireNonNull(user, "user 는 null 일 수 없습니다");
    Role role = Objects.requireNonNull(user.getRole(), "role 은 null 일 수 없습니다");
    Collection<? extends GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role.name()));
    return new JwtUserDetails(
        user.getId(),
        user.getEmail(),
        user.getPassword(),
        user.getName(),
        user.getStatus(),
        authorities);
  }

  /**
   * JWT 발급 시 사용할 JwtPrincipal 을 생성합니다.
   */
  public JwtPrincipal toJwtPrincipal(String deviceId) {
    Collection<String> authorityStrings = authorities.stream()
        .map(GrantedAuthority::getAuthority)
        .collect(Collectors.toUnmodifiableList());
    return new JwtPrincipal(userId, email, displayName, deviceId, authorityStrings);
  }

  public Long getUserId() {
    return userId;
  }

  public MemberStatus getStatus() {
    return status;
  }

  public String getDisplayName() {
    return displayName;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public String getUsername() {
    return email;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true; // 별도 만료 정책이 없어 항상 true
  }

  @Override
  public boolean isAccountNonLocked() {
    return status != MemberStatus.INACTIVE && status != MemberStatus.WITHDRAWN; // 휴면/탈퇴 계정은 잠금 처리
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true; // 비밀번호 만료 정책 미적용
  }

  @Override
  public boolean isEnabled() {
    return status == null || status == MemberStatus.ACTIVE; // 상태 정보 없으면 활성, ACTIVE 만 true
  }
}
