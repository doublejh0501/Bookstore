package com.example.Bookstore.domain.user;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "role", uniqueConstraints = {
        @UniqueConstraint(name = "uk_role_name", columnNames = {"name"})
})
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name; // e.g., ROLE_USER, ROLE_ADMIN
}
