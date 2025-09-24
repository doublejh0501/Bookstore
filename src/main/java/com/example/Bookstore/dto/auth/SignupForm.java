package com.example.Bookstore.dto.auth;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupForm {

    @NotBlank(message = "이름을 입력해주세요.")
    @Size(max = 80, message = "이름은 80자 이하여야 합니다.")
    private String name;

    @NotBlank(message = "이메일을 입력해주세요.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @Size(max = 150, message = "이메일은 150자 이하여야 합니다.")
    private String email;

    @Size(max = 30, message = "연락처는 30자 이하여야 합니다.")
    @Pattern(regexp = "^$|^[0-9\\-]{9,30}$", message = "연락처는 숫자와 하이픈만 사용할 수 있습니다.")
    private String contact;

    @Size(max = 200, message = "주소는 200자 이하여야 합니다.")
    private String address;

    @NotBlank(message = "비밀번호를 입력해주세요.")
    @Size(min = 8, max = 60, message = "비밀번호는 8~60자 사이여야 합니다.")
    private String password;

    @NotBlank(message = "비밀번호 확인을 입력해주세요.")
    private String confirmPassword;

    @AssertTrue(message = "비밀번호가 일치하지 않습니다.")
    public boolean isPasswordConfirmed() {
        if (password == null || confirmPassword == null) {
            return false;
        }
        return password.equals(confirmPassword);
    }
}
