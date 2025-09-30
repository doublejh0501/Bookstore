package com.example.Bookstore.security.jwt; // HttpOnly 쿠키에 담긴 액세스 토큰으로 인증을 처리하는 필터

import jakarta.servlet.FilterChain; // 필터 체인 제어
import jakarta.servlet.ServletException; // 서블릿 예외 처리
import jakarta.servlet.http.Cookie; // 쿠키 접근을 위해 사용
import jakarta.servlet.http.HttpServletRequest; // 요청 객체
import jakarta.servlet.http.HttpServletResponse; // 응답 객체
import java.io.IOException; // IO 예외
import java.util.Arrays; // 쿠키 배열 탐색에 사용
import java.util.Collection; // 권한 목록 생성
import java.util.Objects; // 널 처리
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken; // SecurityContext 에 저장할 인증 토큰
import org.springframework.security.core.context.SecurityContextHolder; // 현재 쓰레드의 인증 컨텍스트
import org.springframework.security.core.authority.SimpleGrantedAuthority; // 문자열 권한 → GrantedAuthority 변환
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource; // 요청 세부정보를 Authentication 에 첨부
import org.springframework.util.StringUtils; // 문자열 공백 여부 판별
import org.springframework.web.filter.OncePerRequestFilter; // 요청당 한 번 실행되는 필터 기반 클래스

/**
 * 매 요청마다 HttpOnly 쿠키에서 액세스 토큰을 꺼내 검증하고 SecurityContext 를 구성하는 필터입니다.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  public static final String JWT_EXCEPTION_ATTRIBUTE = "JWT_EXCEPTION"; // 이후 EntryPoint 에서 참조할 속성명

  private final JwtTokenProvider jwtTokenProvider; // 토큰 파싱/검증 컴포넌트
  private final JwtProperties jwtProperties; // 쿠키 이름 등의 설정 값 제공

  public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, JwtProperties jwtProperties) {
    this.jwtTokenProvider = Objects.requireNonNull(jwtTokenProvider, "jwtTokenProvider 는 null 일 수 없습니다");
    this.jwtProperties = Objects.requireNonNull(jwtProperties, "jwtProperties 는 null 일 수 없습니다");
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (SecurityContextHolder.getContext().getAuthentication() != null) {
      filterChain.doFilter(request, response); // 이미 인증된 요청이면 다음 필터로 바로 진행
      return;
    }

    String token = extractTokenFromCookie(request); // 쿠키에서 액세스 토큰 추출
    if (!StringUtils.hasText(token)) {
      filterChain.doFilter(request, response); // 토큰이 없으면 익명 요청으로 처리
      return;
    }

    try {
      JwtClaims claims = jwtTokenProvider.parseAccessToken(token); // 토큰 검증 및 클레임 추출
      UsernamePasswordAuthenticationToken authentication = buildAuthentication(claims, token, request);
      SecurityContextHolder.getContext().setAuthentication(authentication); // SecurityContext 에 인증 정보 저장
    } catch (JwtAuthenticationException ex) {
      request.setAttribute(JWT_EXCEPTION_ATTRIBUTE, ex); // ExceptionHandler 에서 사용할 수 있도록 저장
      SecurityContextHolder.clearContext(); // 혹시 모를 이전 인증 정보 제거
    }

    filterChain.doFilter(request, response);
  }

  private UsernamePasswordAuthenticationToken buildAuthentication(
      JwtClaims claims,
      String token,
      HttpServletRequest request) {
    Collection<SimpleGrantedAuthority> authorities = claims.authorities().stream()
        .map(SimpleGrantedAuthority::new)
        .toList();
    JwtPrincipal principal = new JwtPrincipal(
        claims.userId(),
        claims.email(),
        claims.displayName(),
        claims.deviceId(),
        claims.authorities());
    UsernamePasswordAuthenticationToken authenticationToken =
        new UsernamePasswordAuthenticationToken(principal, token, authorities);
    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
    return authenticationToken;
  }

  private String extractTokenFromCookie(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null || cookies.length == 0) {
      return null;
    }
    String cookieName = jwtProperties.getAccessToken().getCookieName();
    return Arrays.stream(cookies)
        .filter(cookie -> cookieName.equals(cookie.getName()))
        .map(Cookie::getValue)
        .filter(StringUtils::hasText)
        .findFirst()
        .orElse(null);
  }
}
