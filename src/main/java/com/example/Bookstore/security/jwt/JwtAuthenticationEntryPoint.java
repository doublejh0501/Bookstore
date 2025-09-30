package com.example.Bookstore.security.jwt; // 인증 실패(401) 응답을 표준화하기 위한 EntryPoint

import com.fasterxml.jackson.databind.ObjectMapper; // JSON 응답 직렬화에 사용
import jakarta.servlet.ServletException; // 서블릿 예외 정의
import jakarta.servlet.http.HttpServletRequest; // 요청 객체
import jakarta.servlet.http.HttpServletResponse; // 응답 객체
import java.io.IOException; // IO 예외 처리
import java.util.HashMap; // 응답 본문 구성
import java.util.Map; // 응답 본문 인터페이스
import org.springframework.http.MediaType; // Content-Type 지정
import org.springframework.security.core.AuthenticationException; // 인증 예외
import org.springframework.security.web.AuthenticationEntryPoint; // EntryPoint 인터페이스

/**
 * 인증되지 않은 요청이 보호 자원에 접근했을 때 401 JSON 응답을 반환합니다.
 */
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper(); // 재사용 가능한 ObjectMapper

  @Override
  public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
      throws IOException, ServletException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);

    String message = "인증이 필요합니다.";
    Object attribute = request.getAttribute(JwtAuthenticationFilter.JWT_EXCEPTION_ATTRIBUTE);
    if (attribute instanceof JwtAuthenticationException jwtEx) {
      message = jwtEx.getMessage();
    } else if (authException.getMessage() != null) {
      message = authException.getMessage();
    }

    Map<String, Object> body = new HashMap<>();
    body.put("error", "unauthorized");
    body.put("message", message);
    body.put("status", HttpServletResponse.SC_UNAUTHORIZED);

    OBJECT_MAPPER.writeValue(response.getOutputStream(), body);
  }
}
