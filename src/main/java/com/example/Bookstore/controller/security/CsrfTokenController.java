package com.example.Bookstore.controller.security; // CSRF 토큰 발급용 컨트롤러 패키지

import java.util.Map; // 응답을 간단한 맵으로 반환
import org.springframework.security.web.csrf.CsrfToken; // 현재 요청의 CSRF 토큰 정보
import org.springframework.web.bind.annotation.GetMapping; // GET 요청 매핑
import org.springframework.web.bind.annotation.RestController; // JSON 응답용 컨트롤러

/**
 * 프론트엔드에서 필요할 때마다 새 CSRF 토큰을 조회할 수 있는 간단한 엔드포인트입니다.
 * CookieCsrfTokenRepository와 함께 사용되며, 호출 시 쿠키와 파라미터 이름, 실제 토큰 값을 돌려줍니다.
 */
@RestController
public class CsrfTokenController {

  @GetMapping("/csrf-token")
  public Map<String, String> csrfToken(CsrfToken token) {
    return Map.of(
        "token", token.getToken(),
        "parameterName", token.getParameterName(),
        "headerName", token.getHeaderName());
  }
}
