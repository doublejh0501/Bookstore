package com.example.Bookstore.domain.book;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "best_book")
public class BestBook {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    // 'rank' is a reserved keyword in MySQL (window function). Use a safe column name.
    @Column(name = "ranking")
    private Long ranking;

    @Column(name = "sellcount")
    private Long sellCount;
}
