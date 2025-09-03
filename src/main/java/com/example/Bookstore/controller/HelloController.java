package com.example.Bookstore.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HelloController {

    @GetMapping("/hello")
    public String hello(Authentication authentication) {
        return "Hello, " + (authentication != null ? authentication.getName() : "anonymous") + "!";
    }

    @GetMapping("/public")
    public String publicEndpoint() {
        return "This is public.";
    }
}

