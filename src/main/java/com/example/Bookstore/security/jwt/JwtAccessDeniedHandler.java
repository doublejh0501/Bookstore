package com.example.Bookstore.security.jwt; // 인가 실패(403) 응답을 담당하는 핸들러

import com.fasterxml.jackson.databind.ObjectMapper; // JSON 응답 생성
import jakarta.servlet.ServletException; // 서블릿 예외 정의
import jakarta.servlet.http.HttpServletRequest; // 요청 객체
import jakarta.servlet.http.HttpServletResponse; // 응답 객체
import java.io.IOException; // IO 예외 처리
import java.util.HashMap; // 응답 본문 구성
import java.util.Map; // 응답 본문 인터페이스
import org.springframework.http.MediaType; // Content-Type 지정
import org.springframework.security.access.AccessDeniedException; // 접근 거부 예외
import org.springframework.security.web.access.AccessDeniedHandler; // AccessDeniedHandler 인터페이스

/**
 * 인증은 되었지만 권한이 부족한 경우 403 JSON 응답을 반환합니다.
 */
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException) throws IOException, ServletException {
    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);

    Map<String, Object> body = new HashMap<>();
    body.put("error", "forbidden");
    body.put("message", accessDeniedException.getMessage());
    body.put("status", HttpServletResponse.SC_FORBIDDEN);

    OBJECT_MAPPER.writeValue(response.getOutputStream(), body);
  }
}
