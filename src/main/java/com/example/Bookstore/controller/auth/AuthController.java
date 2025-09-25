package com.example.Bookstore.controller.auth;

import com.example.Bookstore.dto.auth.SignupForm;
import com.example.Bookstore.service.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

import java.util.Map;

@Controller
@RequestMapping
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

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
