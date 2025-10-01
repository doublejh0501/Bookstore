package com.example.Bookstore.security.csrf;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.lang.NonNull;

/**
 * Ensures the CsrfToken is accessed so that CookieCsrfTokenRepository reissues the cookie after stateless logins.
 */
public class CsrfCookieRefreshFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain) throws ServletException, IOException {
    CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
    if (csrfToken != null) {
      // JWT 로그인 이후 쿠키를 새로 발급받기 위해 토큰을 한 번 읽어 CookieCsrfTokenRepository를 동작시킨다.
      // 새 쿠키가 없으면 이후 로그아웃 요청이 CSRF 검증에서 실패한다.
      csrfToken.getToken();
    }
    filterChain.doFilter(request, response);
  }
}
