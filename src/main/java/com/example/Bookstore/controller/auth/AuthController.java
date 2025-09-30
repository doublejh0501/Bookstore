package com.example.Bookstore.controller.auth;

import com.example.Bookstore.dto.auth.LoginForm;
import com.example.Bookstore.dto.auth.SignupForm;
import com.example.Bookstore.security.auth.AuthService;
import com.example.Bookstore.security.jwt.JwtAuthenticationException;
import com.example.Bookstore.security.jwt.JwtCookieService;
import com.example.Bookstore.service.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 회원가입 + 로그인/로그아웃 + 토큰 재발급을 처리하는 컨트롤러입니다.
 */
@Controller
@RequestMapping
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthService authService;
    private final JwtCookieService jwtCookieService;

    @ModelAttribute("login")
    public LoginForm loginForm() {
        return new LoginForm();
    }

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @PostMapping("/login")
    public String login(@Valid @ModelAttribute("login") LoginForm form,
                        BindingResult bindingResult,
                        HttpServletResponse response,
                        RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            form.setPassword(null);
            return "auth/login";
        }

        try {
            AuthService.LoginResult result = authService.login(
                    new AuthService.LoginCommand(form.getEmail(), form.getPassword(), form.getDeviceId()));

            jwtCookieService.writeAccessToken(response,
                    result.tokenPair().accessToken(), result.tokenPair().accessTokenExpiresAt());
            jwtCookieService.writeRefreshToken(response,
                    result.tokenPair().refreshToken(), result.tokenPair().refreshTokenExpiresAt());

            redirectAttributes.addFlashAttribute("loginSuccess", true);
            return "redirect:/";
        } catch (AuthenticationException ex) {
            bindingResult.reject("loginFailed", "이메일 또는 비밀번호가 올바르지 않습니다.");
            form.setPassword(null);
            return "auth/login";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request,
                         HttpServletResponse response,
                         RedirectAttributes redirectAttributes) {
        jwtCookieService.extractRefreshToken(request).ifPresent(authService::logout);
        jwtCookieService.clearAccessToken(response);
        jwtCookieService.clearRefreshToken(response);
        redirectAttributes.addFlashAttribute("logoutSuccess", true);
        return "redirect:/login?logout";
    }

    @PostMapping("/auth/token/refresh")
    @ResponseBody
    public ResponseEntity<?> refreshToken(HttpServletRequest request,
                                          HttpServletResponse response) {
        return jwtCookieService.extractRefreshToken(request)
                .map(token -> {
                    try {
                        var rotation = authService.refresh(token);
                        jwtCookieService.writeAccessToken(response,
                                rotation.newAccessToken(), rotation.newAccessTokenExpiresAt());
                        jwtCookieService.writeRefreshToken(response,
                                rotation.newRefreshToken(), rotation.newRefreshTokenExpiresAt());
                        return ResponseEntity.noContent().build();
                    } catch (JwtAuthenticationException ex) {
                        authService.logout(token);
                        jwtCookieService.clearAccessToken(response);
                        jwtCookieService.clearRefreshToken(response);
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(Map.of(
                                        "error", "invalid_refresh_token",
                                        "message", ex.getMessage()));
                    }
                })
                .orElseGet(() -> {
                    jwtCookieService.clearAccessToken(response);
                    jwtCookieService.clearRefreshToken(response);
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(Map.of(
                                    "error", "missing_refresh_token",
                                    "message", "리프레시 토큰이 필요합니다."));
                });
    }

    @GetMapping("/signup")
    public String signupForm(Model model) {
        if (!model.containsAttribute("signup")) {
            model.addAttribute("signup", new SignupForm());
        }
        return "auth/signup";
    }

    @PostMapping("/signup")
    public String signup(@Valid @ModelAttribute("signup") SignupForm form,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes,
                         Model model) {

        if (!userService.isEmailAvailable(form.getEmail())) {
            bindingResult.rejectValue("email", "duplicate", "이미 사용 중인 이메일입니다.");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("signup", form);
            return "auth/signup";
        }

        try {
            userService.register(form);
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("signupFailed", ex.getMessage());
            model.addAttribute("signup", form);
            return "auth/signup";
        }

        redirectAttributes.addFlashAttribute("signupSuccess", true);
        return "redirect:/login";
    }

    @GetMapping("/signup/check-email")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkEmailDuplicate(@RequestParam("email") String email) {
        boolean available = userService.isEmailAvailable(email);
        return ResponseEntity.ok(Map.of(
                "email", email,
                "available", available
        ));
    }
}
