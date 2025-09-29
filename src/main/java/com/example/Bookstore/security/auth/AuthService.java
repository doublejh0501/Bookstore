package com.example.Bookstore.security.auth; // 로그인/재발급/로그아웃 핵심 비즈니스 로직을 담당하는 서비스

import com.example.Bookstore.domain.user.User; // 마지막 로그인 시각 업데이트를 위해 사용
import com.example.Bookstore.repository.user.UserRepository; // 사용자 쓰기 연산을 위해 사용
import com.example.Bookstore.security.jwt.JwtAuthenticationException; // JWT 관련 예외 처리에 사용
import com.example.Bookstore.security.jwt.JwtTokenProvider; // 토큰 발급/검증 컴포넌트
import java.time.Clock; // 테스트 가능한 현재 시각 주입을 위해 사용
import java.time.LocalDateTime; // 마지막 로그인 시각 기록에 사용
import java.util.Objects; // null 검증
import java.util.UUID; // deviceId 생성 시 사용
import org.springframework.security.authentication.AuthenticationManager; // 스프링 인증 진입점
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken; // 사용자명/비밀번호 인증 토큰
import org.springframework.security.core.Authentication; // 인증 결과 표현
import org.springframework.stereotype.Service; // 스프링 서비스 빈 등록
import org.springframework.transaction.annotation.Transactional; // 트랜잭션 경계 설정
import org.springframework.util.StringUtils; // 문자열 공백 여부 체크

/**
 * 로그인 성공 시 JWT 를 발급하고, 리프레시 토큰 회전 및 로그아웃을 처리하는 서비스입니다.
 */
@Service
public class AuthService {

  private final AuthenticationManager authenticationManager; // 스프링 Security 인증 관리자
  private final JwtTokenProvider jwtTokenProvider; // JWT 생성/검증 유틸리티
  private final UserRepository userRepository; // 사용자 엔티티 쓰기 작업
  private final Clock clock; // 마지막 로그인 기록에 사용할 시계

  public AuthService(
      AuthenticationManager authenticationManager,
      JwtTokenProvider jwtTokenProvider,
      UserRepository userRepository,
      Clock clock) {
    this.authenticationManager = Objects.requireNonNull(authenticationManager, "authenticationManager 는 null 일 수 없습니다");
    this.jwtTokenProvider = Objects.requireNonNull(jwtTokenProvider, "jwtTokenProvider 는 null 일 수 없습니다");
    this.userRepository = Objects.requireNonNull(userRepository, "userRepository 는 null 일 수 없습니다");
    this.clock = Objects.requireNonNull(clock, "clock 는 null 일 수 없습니다");
  }

  /**
   * 이메일/비밀번호를 검증해 JWT 토큰 쌍을 발급합니다.
   */
  @Transactional
  public LoginResult login(LoginCommand command) {
    Objects.requireNonNull(command, "command 는 null 일 수 없습니다");
    UsernamePasswordAuthenticationToken authenticationRequest = new UsernamePasswordAuthenticationToken(
        command.email(), command.password());

    Authentication authentication = authenticationManager.authenticate(authenticationRequest); // AuthenticationManager 를 통해 사용자 검증
    JwtUserDetails userDetails = (JwtUserDetails) authentication.getPrincipal(); // 인증 성공 시 UserDetails 확보

    String deviceId = resolveDeviceId(command.deviceId());
    JwtTokenProvider.IssuedTokenPair tokenPair = jwtTokenProvider.issueTokens(
        userDetails.toJwtPrincipal(deviceId)); // JWT 발급

    updateLastLogin(userDetails.getUserId()); // 사용자 마지막 로그인 시각 갱신

    return new LoginResult(userDetails, tokenPair, deviceId);
  }

  /**
   * 리프레시 토큰을 검증하고 회전해 새 토큰을 발급합니다.
   */
  @Transactional
  public JwtTokenProvider.RefreshTokenRotationResult refresh(String refreshToken) {
    return jwtTokenProvider.rotateRefreshToken(refreshToken);
  }

  /**
   * 로그아웃 시 리프레시 토큰을 화이트리스트에서 제거합니다.
   * 토큰이 없거나 이미 제거된 경우에도 조용히 지나가도록 설계했습니다.
   */
  @Transactional
  public void logout(String refreshToken) {
    if (!StringUtils.hasText(refreshToken)) {
      return; // 쿠키가 없으면 아무 작업도 하지 않습니다
    }
    try {
      var claims = jwtTokenProvider.parseRefreshToken(refreshToken); // 토큰 검증
      jwtTokenProvider.invalidateRefreshToken(claims.tokenId()); // 화이트리스트에서 제거
    } catch (JwtAuthenticationException ex) {
      // 이미 만료되었거나 변조된 토큰이라면 추가 조치 없이 무시 (감사 로그는 상위에서 처리 가능)
    }
  }

  private void updateLastLogin(Long userId) {
    userRepository.findById(userId).ifPresent(user -> {
      user.setLastLoginAt(LocalDateTime.now(clock));
    });
  }

  private String resolveDeviceId(String deviceId) {
    if (StringUtils.hasText(deviceId)) {
      return deviceId;
    }
    return UUID.randomUUID().toString();
  }

  /**
   * 로그인에 필요한 입력 값을 담는 레코드입니다.
   */
  public record LoginCommand(String email, String password, String deviceId) {
    public LoginCommand {
      Objects.requireNonNull(email, "email 은 null 일 수 없습니다");
      Objects.requireNonNull(password, "password 는 null 일 수 없습니다");
    }
  }

  /**
   * 로그인 성공 시 컨트롤러로 전달할 정보를 묶은 DTO 입니다.
   */
  public record LoginResult(JwtUserDetails userDetails, JwtTokenProvider.IssuedTokenPair tokenPair, String deviceId) {
  }
}
