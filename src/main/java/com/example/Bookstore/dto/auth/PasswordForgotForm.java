package com.example.Bookstore.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 비밀번호 재설정 링크를 요청할 때 사용자가 입력하는 단일 필드 폼입니다.
 */
@Data
public class PasswordForgotForm {

  /**
   * 재설정 안내 메일을 받을 사용자 이메일 주소입니다.
   * <p>
   * {@link NotBlank}와 {@link Email} 어노테이션으로 필수 입력과 형식을 동시에 검증합니다.
   */
  @NotBlank(message = "이메일을 입력해주세요.")
  @Email(message = "올바른 이메일 형식이 아닙니다.")
  private String email;
}
