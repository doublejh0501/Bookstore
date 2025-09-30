package com.example.Bookstore.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.util.StringUtils;

/**
 * 재설정 화면에서 토큰과 새 비밀번호 정보를 받아오는 폼입니다.
 */
@Data
public class PasswordResetForm {

  /**
   * 재설정 메일에 포함된 토큰 문자열입니다.
   * <p>
   * 비밀번호 변경 시 서버가 토큰을 다시 조회해 유효성을 검사합니다.
   */
  @NotBlank(message = "토큰 정보가 필요합니다.")
  private String token;

  /**
   * 사용자가 새로 설정할 비밀번호입니다. 최소 8자, 최대 64자로 제한합니다.
   */
  @NotBlank(message = "새 비밀번호를 입력해주세요.")
  @Size(min = 8, max = 64, message = "비밀번호는 8~64자 사이여야 합니다.")
  private String newPassword;

  /**
   * 새 비밀번호 확인 입력란입니다. {@link #isPasswordMatched()}에서 비교합니다.
   */
  @NotBlank(message = "비밀번호 확인을 입력해주세요.")
  private String confirmNewPassword;

  /**
   * 두 비밀번호 필드가 모두 공백이 아니면서 완전히 같은지 검사합니다.
   */
  public boolean isPasswordMatched() {
    return StringUtils.hasText(newPassword)
        && StringUtils.hasText(confirmNewPassword)
        && newPassword.equals(confirmNewPassword);
  }
}
