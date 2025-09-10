package com.example.Bookstore.config;

import com.example.Bookstore.domain.user.Role;
import com.example.Bookstore.domain.user.User;
import com.example.Bookstore.repository.user.RoleRepository;
import com.example.Bookstore.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_USER").build()));
        roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_ADMIN").build()));

        List<User> users = userRepository.findAll();
        for (User u : users) {
            if (u.getRoles() == null || u.getRoles().isEmpty()) {
                u.getRoles().add(userRole);
            }
        }
        userRepository.saveAll(users);
    }
}

