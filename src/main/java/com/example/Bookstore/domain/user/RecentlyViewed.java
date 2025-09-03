package com.example.Bookstore.domain.user;

import com.example.Bookstore.domain.book.Book;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "recently_viewed", uniqueConstraints = {
        @UniqueConstraint(name = "uk_recently_viewed_user_book", columnNames = {"user_id", "book_id"})
})
public class RecentlyViewed {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(nullable = false)
    private LocalDateTime viewedAt;

    @PrePersist
    @PreUpdate
    public void touch() {
        if (viewedAt == null) viewedAt = LocalDateTime.now();
    }
}

