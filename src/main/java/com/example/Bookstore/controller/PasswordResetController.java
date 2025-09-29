package com.example.Bookstore.controller;

import com.example.Bookstore.dto.auth.PasswordForgotForm;
import com.example.Bookstore.dto.auth.PasswordResetForm;
import com.example.Bookstore.service.user.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 비밀번호 찾기(재설정 링크 발송)와 재설정 폼 제출을 처리하는 MVC 컨트롤러입니다.
 * <p>
 * URL 구조는 {@code /password/forgot}, {@code /password/reset} 으로 구성되어 있으며,
 * 각 핸들러가 {@link PasswordResetService} 를 호출하여 실제 비즈니스 로직을 위임합니다.
 */
@Controller
@RequestMapping("/password")
@RequiredArgsConstructor
public class PasswordResetController {

  private final PasswordResetService passwordResetService;

  /**
   * 비밀번호 찾기 화면에서 사용할 폼 객체를 항상 모델에 넣어 둡니다.
   * <p>
   * 스프링 MVC는 {@code @ModelAttribute} 로 선언된 메서드를 각 요청 전에 호출하여
   * 반환값을 자동으로 모델에 추가합니다.
   */
  @ModelAttribute("forgotForm")
  public PasswordForgotForm forgotForm() {
    return new PasswordForgotForm();
  }

  /**
   * 재설정 화면에서 사용될 폼 객체를 미리 준비합니다.
   * <p>
   * 토큰 값은 {@link #showReset(String, Model)} 안에서 주입합니다.
   */
  @ModelAttribute("resetForm")
  public PasswordResetForm resetForm() {
    return new PasswordResetForm();
  }

  /**
   * 비밀번호 찾기(이메일 입력) 화면을 보여줍니다.
   */
  @GetMapping("/forgot")
  public String showForgot() {
    return "auth/password-forgot";
  }

  /**
   * 사용자가 입력한 이메일을 검증하고, 유효하다면 재설정 링크 메일을 보냅니다.
   *
   * @param form             사용자가 제출한 이메일 한 건만을 담는 폼 객체
   * @param bindingResult    폼 검증 결과(필수 입력, 이메일 형식 등)를 담는 객체
   * @param redirectAttributes 리다이렉트 시 플래시 속성으로 안내 메시지를 전달하기 위한 객체
   * @return 유효성 오류가 있으면 동일 화면을, 성공하면 다시 {@code /password/forgot} 로 리다이렉트합니다.
   */
  @PostMapping("/forgot")
  public String handleForgot(
      @Valid @ModelAttribute("forgotForm") PasswordForgotForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes) {

    if (bindingResult.hasErrors()) {
      // 폼 검증에 실패하면 동일 페이지를 다시 렌더링합니다.
      return "auth/password-forgot";
    }

    passwordResetService.sendResetLink(form.getEmail());
    redirectAttributes.addFlashAttribute("sent", true);
    return "redirect:/password/forgot";
  }

  /**
   * 메일에 포함된 토큰으로 접근했을 때 재설정 화면을 렌더링합니다.
   *
   * @param token 메일에 담긴 재설정 토큰(없을 수도 있음)
   * @param model 뷰에 전달할 데이터 추가 용도
   */
  @GetMapping("/reset")
  public String showReset(@RequestParam(name = "token", required = false) String token, Model model) {
    if (token != null) {
      boolean tokenValid = passwordResetService.isTokenUsable(token);
      model.addAttribute("token", token);
      model.addAttribute("tokenValid", tokenValid);
      if (!tokenValid) {
        model.addAttribute("tokenError", "토큰이 만료되었거나 유효하지 않습니다. 다시 요청해주세요.");
      }

      PasswordResetForm form = (PasswordResetForm) model.asMap().get("resetForm");
      if (form == null) {
        form = new PasswordResetForm();
      }
      form.setToken(token);
      model.addAttribute("resetForm", form);
    } else {
      // 토큰 없이 접근한 경우에는 토큰이 없음을 명시해 안내 메시지를 띄웁니다.
      model.addAttribute("tokenValid", false);
    }
    return "auth/password-reset";
  }

  /**
   * 사용자가 새 비밀번호를 제출했을 때 호출됩니다.
   * <p>
   * 비밀번호 일치 여부와 토큰 유효성을 검증하고 문제가 없으면 실제 비밀번호를 변경합니다.
   */
  @PostMapping("/reset")
  public String handleReset(
      @Valid @ModelAttribute("resetForm") PasswordResetForm form,
      BindingResult bindingResult,
      Model model) {

    if (!form.isPasswordMatched()) {
      bindingResult.rejectValue("confirmNewPassword", "password.mismatch", "비밀번호가 서로 일치하지 않습니다.");
    }

    if (bindingResult.hasErrors()) {
      // 검증 실패 시에는 다시 폼을 보여 줄 수 있도록 필요한 데이터(토큰, 유효 여부)를 준비합니다.
      model.addAttribute("token", form.getToken());
      model.addAttribute("tokenValid", passwordResetService.isTokenUsable(form.getToken()));
      return "auth/password-reset";
    }

    try {
      passwordResetService.resetPassword(form);
    } catch (IllegalArgumentException | IllegalStateException ex) {
      // 토큰 만료/중복 사용 등 비즈니스 예외는 Global Error 로 전달하여 화면에서 출력합니다.
      bindingResult.reject("token.invalid", ex.getMessage());
      model.addAttribute("token", form.getToken());
      model.addAttribute("tokenValid", false);
      return "auth/password-reset";
    }

    model.addAttribute("resetSuccess", true);
    model.addAttribute("tokenValid", false);
    return "auth/password-reset";
  }
}
