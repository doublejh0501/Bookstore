package com.example.Bookstore.dto.auth; // 로그인 폼 데이터를 전달하는 DTO 패키지

import jakarta.validation.constraints.Email; // 이메일 형식 검증
import jakarta.validation.constraints.NotBlank; // 필수 입력 검증
import lombok.Getter; // 뷰에서 폼 바인딩을 간편하게 하기 위한 Lombok
import lombok.Setter; // 폼 데이터를 수정할 수 있도록 Setter 사용

/**
 * Thymeleaf 로그인 폼에서 이메일/비밀번호를 바인딩하기 위한 DTO 입니다.
 */
@Getter
@Setter
public class LoginForm {

  @NotBlank(message = "이메일을 입력해주세요")
  @Email(message = "올바른 이메일 형식이 아닙니다")
  private String email; // 로그인 아이디로 사용할 이메일

  @NotBlank(message = "비밀번호를 입력해주세요")
  private String password; // 평문 비밀번호 (컨트롤러에서 인증 후 즉시 폐기)

  private String deviceId; // 선택적으로 전달되는 기기 식별자 (없으면 서버에서 생성)
}
