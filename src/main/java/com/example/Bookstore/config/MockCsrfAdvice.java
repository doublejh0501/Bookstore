package com.example.Bookstore.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class MockCsrfAdvice {

    @Getter
    @AllArgsConstructor
    static class MockCsrfToken {
        private final String parameterName;
        private final String token;
    }

    @ModelAttribute("_csrf")
    public MockCsrfToken csrf() {
        // Provide a dummy CSRF object so forms render without errors in simple view-only mapping
        return new MockCsrfToken("_csrf", "demo-csrf-token");
    }
}

