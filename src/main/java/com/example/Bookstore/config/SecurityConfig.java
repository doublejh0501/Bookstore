package com.example.Bookstore.config; // 보안 전반 설정 패키지

import com.example.Bookstore.security.auth.JwtUserDetailsService; // DaoAuthenticationProvider 구성에 사용
import com.example.Bookstore.security.csrf.CsrfCookieRefreshFilter; // 로그인 후 CSRF 쿠키 재발급 필터
import com.example.Bookstore.security.jwt.JwtAccessDeniedHandler; // 403 응답 처리 핸들러
import com.example.Bookstore.security.jwt.JwtAuthenticationEntryPoint; // 401 응답 처리 핸들러
import com.example.Bookstore.security.jwt.JwtAuthenticationFilter; // HttpOnly 쿠키 기반 JWT 인증 필터
import com.example.Bookstore.security.jwt.JwtProperties; // JWT 설정 값
import com.example.Bookstore.security.jwt.JwtTokenProvider; // JWT 파싱/생성 유틸
import java.time.Clock; // 공용 Clock 빈 생성
import org.springframework.boot.autoconfigure.security.servlet.PathRequest; // 정적 리소스 화이트리스트 지정에 사용
import org.springframework.context.annotation.Bean; // 빈 등록용 어노테이션
import org.springframework.context.annotation.Configuration; // 구성 클래스 선언
import org.springframework.security.authentication.AuthenticationManager; // AuthenticationManager Bean
import org.springframework.security.authentication.ProviderManager; // DaoAuthenticationProvider 를 감싸는 매니저
import org.springframework.security.authentication.dao.DaoAuthenticationProvider; // UserDetailsService 기반 인증 제공자
import org.springframework.security.config.annotation.web.builders.HttpSecurity; // SecurityFilterChain 구성에 사용
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity; // 웹 보안 활성화 어노테이션
import org.springframework.security.config.http.SessionCreationPolicy; // 세션 Stateless 설정
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; // 비밀번호 암호화 구현체
import org.springframework.security.crypto.password.PasswordEncoder; // 비밀번호 인코더 인터페이스
import org.springframework.security.web.SecurityFilterChain; // 최종 보안 필터 체인
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter; // 커스텀 필터 위치 지정
import org.springframework.security.web.csrf.CookieCsrfTokenRepository; // 쿠키 기반 CSRF 토큰 저장소
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler; // CSRF 토큰 요청 속성 핸들러
import org.springframework.security.web.csrf.CsrfFilter; // CSRF 필터 위치 참조

/**
 * 전체 애플리케이션의 보안 규칙과 인증 관련 빈을 구성합니다.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      JwtAuthenticationFilter jwtAuthenticationFilter,
      DaoAuthenticationProvider authenticationProvider,
      JwtAuthenticationEntryPoint authenticationEntryPoint,
      JwtAccessDeniedHandler accessDeniedHandler,
      JwtProperties jwtProperties) throws Exception {

    CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse(); // JS 에서도 읽을 수 있도록 설정
    csrfTokenRepository.setCookiePath(jwtProperties.getAccessToken().getCookiePath()); // 쿠키 경로를 액세스 토큰과 일치시켜 정리 용이
    csrfTokenRepository.setCookieName("BOOKSTORE_CSRF"); // 명시적 이름 부여로 관리 편의성 향상

    CsrfTokenRequestAttributeHandler csrfTokenRequestHandler = new CsrfTokenRequestAttributeHandler();

    http
        .csrf(csrf -> csrf
            .csrfTokenRepository(csrfTokenRepository)
            .csrfTokenRequestHandler(csrfTokenRequestHandler))
        .addFilterAfter(new CsrfCookieRefreshFilter(), CsrfFilter.class)
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authenticationProvider(authenticationProvider)
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint(authenticationEntryPoint)
            .accessDeniedHandler(accessDeniedHandler))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
            .requestMatchers(
                "/", "/login", "/signup", "/signup/**", "/password/**",
                "/error", "/error/**", "/css/**", "/js/**", "/images/**",
                "/csrf-token", "/books/**").permitAll()
            .requestMatchers("/admin/**").hasRole("ADMIN")
            .anyRequest().authenticated())
        .formLogin(form -> form.disable())
        .httpBasic(httpBasic -> httpBasic.disable())
        .logout(logout -> logout.disable());

    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public DaoAuthenticationProvider daoAuthenticationProvider(
      JwtUserDetailsService userDetailsService,
      PasswordEncoder passwordEncoder) {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
    provider.setUserDetailsService(userDetailsService);
    provider.setPasswordEncoder(passwordEncoder);
    provider.setHideUserNotFoundExceptions(false); // 이메일 존재 여부를 명확하게 드러내도록 설정
    return provider;
  }

  @Bean
  public AuthenticationManager authenticationManager(DaoAuthenticationProvider authenticationProvider) {
    return new ProviderManager(authenticationProvider);
  }

  @Bean
  public JwtAuthenticationFilter jwtAuthenticationFilter(
      JwtTokenProvider jwtTokenProvider,
      JwtProperties jwtProperties) {
    return new JwtAuthenticationFilter(jwtTokenProvider, jwtProperties);
  }

  @Bean
  public JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint() {
    return new JwtAuthenticationEntryPoint();
  }

  @Bean
  public JwtAccessDeniedHandler jwtAccessDeniedHandler() {
    return new JwtAccessDeniedHandler();
  }

  @Bean
  public Clock systemClock() {
    return Clock.systemUTC();
  }
}
