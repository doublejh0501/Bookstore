package com.example.Bookstore.security.jwt; // JWT 쿠키 발급/삭제를 담당하는 헬퍼 서비스

import jakarta.servlet.http.HttpServletRequest; // 쿠키 추출용 요청 객체
import jakarta.servlet.http.HttpServletResponse; // Set-Cookie 헤더 추가용 응답 객체
import java.time.Clock; // 현재 시각 계산에 사용
import java.time.Duration; // Max-Age 계산용
import java.time.Instant; // 만료 시각 계산용
import java.util.Arrays; // 쿠키 배열 탐색에 사용
import java.util.Optional; // 쿠키 존재 여부 표현
import org.springframework.beans.factory.annotation.Autowired; // 생성자 주입 지정
import org.springframework.http.HttpHeaders; // Set-Cookie 헤더 상수
import org.springframework.http.ResponseCookie; // SameSite 등 현대 브라우저 속성 지원
import org.springframework.stereotype.Component; // 스프링 컴포넌트 등록
import org.springframework.util.StringUtils; // 문자열 공백 여부 체크

/**
 * 액세스/리프레시 토큰을 HttpOnly 쿠키로 발급하거나 제거하는 책임을 담당합니다.
 */
@Component
public class JwtCookieService {

  private final JwtProperties jwtProperties; // 쿠키 속성 정보 제공
  private final Clock clock; // 현재 시각 계산용 시계 (테스트 시 주입 가능)

  public JwtCookieService(JwtProperties jwtProperties) {
    this(jwtProperties, Clock.systemDefaultZone());
  }

  @Autowired
  public JwtCookieService(JwtProperties jwtProperties, Clock clock) {
    this.jwtProperties = jwtProperties;
    this.clock = clock;
  }

  public void writeAccessToken(HttpServletResponse response, String tokenValue, Instant expiresAt) {
    var props = jwtProperties.getAccessToken();
    Duration maxAge = remaining(expiresAt);
    ResponseCookie cookie = ResponseCookie.from(props.getCookieName(), tokenValue)
        .httpOnly(props.isCookieHttpOnly())
        .secure(props.isCookieSecure())
        .path(props.getCookiePath())
        .sameSite(props.getNormalizedSameSite())
        .maxAge(maxAge)
        .build();
    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
  }

  public void writeRefreshToken(HttpServletResponse response, String tokenValue, Instant expiresAt) {
    var props = jwtProperties.getRefreshToken();
    Duration maxAge = remaining(expiresAt);
    ResponseCookie cookie = ResponseCookie.from(props.getCookieName(), tokenValue)
        .httpOnly(props.isCookieHttpOnly())
        .secure(props.isCookieSecure())
        .path(props.getCookiePath())
        .sameSite(props.getNormalizedSameSite())
        .maxAge(maxAge)
        .build();
    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
  }

  public void clearAccessToken(HttpServletResponse response) {
    var props = jwtProperties.getAccessToken();
    ResponseCookie cookie = ResponseCookie.from(props.getCookieName(), "")
        .httpOnly(props.isCookieHttpOnly())
        .secure(props.isCookieSecure())
        .path(props.getCookiePath())
        .sameSite(props.getNormalizedSameSite())
        .maxAge(Duration.ZERO)
        .build();
    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
  }

  public void clearRefreshToken(HttpServletResponse response) {
    var props = jwtProperties.getRefreshToken();
    ResponseCookie cookie = ResponseCookie.from(props.getCookieName(), "")
        .httpOnly(props.isCookieHttpOnly())
        .secure(props.isCookieSecure())
        .path(props.getCookiePath())
        .sameSite(props.getNormalizedSameSite())
        .maxAge(Duration.ZERO)
        .build();
    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
  }

  public Optional<String> extractRefreshToken(HttpServletRequest request) {
    return extractCookie(request, jwtProperties.getRefreshToken().getCookieName());
  }

  private Duration remaining(Instant expiresAt) {
    Instant now = Instant.now(clock);
    if (expiresAt == null || expiresAt.isBefore(now)) {
      return Duration.ZERO;
    }
    return Duration.between(now, expiresAt);
  }

  private Optional<String> extractCookie(HttpServletRequest request, String cookieName) {
    if (request.getCookies() == null) {
      return Optional.empty();
    }
    return Arrays.stream(request.getCookies())
        .filter(cookie -> cookieName.equals(cookie.getName()))
        .map(cookie -> StringUtils.hasText(cookie.getValue()) ? cookie.getValue() : null)
        .filter(StringUtils::hasText)
        .findFirst();
  }
}
