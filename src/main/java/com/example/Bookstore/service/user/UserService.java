package com.example.Bookstore.service.user;

import com.example.Bookstore.domain.user.Role;
import com.example.Bookstore.domain.user.User;
import com.example.Bookstore.dto.auth.SignupForm;
import com.example.Bookstore.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public boolean isEmailAvailable(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return !userRepository.existsByEmail(email);
    }

    @Transactional
    public void register(SignupForm form) {
        if (!isEmailAvailable(form.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        String normalizedContact = form.getContact();
        if (normalizedContact != null) {
            normalizedContact = normalizedContact.trim();
            if (normalizedContact.isEmpty()) {
                normalizedContact = null;
            } else {
                normalizedContact = normalizedContact.replaceAll("[^0-9]", "");
                if (normalizedContact.isEmpty()) {
                    normalizedContact = null;
                }
            }
        }

        String normalizedAddress = form.getAddress();
        if (normalizedAddress != null) {
            normalizedAddress = normalizedAddress.trim();
            if (normalizedAddress.isEmpty()) {
                normalizedAddress = null;
            }
        }

        User user = User.builder()
                .name(form.getName())
                .email(form.getEmail())
                .mobile(normalizedContact)
                .address(normalizedAddress)
                .password(passwordEncoder.encode(form.getPassword()))
                .role(Role.ROLE_USER)
                .createdAt(LocalDateTime.now())
                .build();
        userRepository.save(user);
    }
}
