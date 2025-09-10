package com.example.Bookstore.domain.user;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
 

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_email", columnNames = {"email"}),
        @UniqueConstraint(name = "uk_users_username", columnNames = {"username"})
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Login/Display ID separate from email (optional but unique if provided)
    @Column(length = 80)
    private String username;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(length = 80)
    private String firstName;

    @Column(length = 80)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 30, nullable = false)
    private Role role;

    // Contact and address info (simplified)
    @Column(length = 30)
    private String phone;

    @Column(length = 200)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private MemberStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private MemberGrade grade;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime lastLoginAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (role == null) role = Role.ROLE_USER;
        if (status == null) status = MemberStatus.ACTIVE;
        if (grade == null) grade = MemberGrade.BASIC;
    }
}
