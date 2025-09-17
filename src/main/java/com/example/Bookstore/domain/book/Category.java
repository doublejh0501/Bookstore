package com.example.Bookstore.domain.book;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "category", uniqueConstraints = {
        @UniqueConstraint(name = "uk_categories_name", columnNames = {"name"})
})
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    // Optional self-reference to support hierarchical categories (large/medium/small)
    @OneToMany(mappedBy = "category")
    private Set<Book> books = new HashSet<>();
}
