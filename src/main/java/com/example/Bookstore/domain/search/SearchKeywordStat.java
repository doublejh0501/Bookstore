package com.example.Bookstore.domain.search;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "search_keyword_stats", indexes = {
        @Index(name = "idx_search_keyword", columnList = "keyword")
})
public class SearchKeywordStat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String keyword;

    @Column(nullable = false)
    private Long count;

    @Column(nullable = false)
    private LocalDateTime lastSearchedAt;

    @PrePersist
    @PreUpdate
    public void touch() {
        if (lastSearchedAt == null) lastSearchedAt = LocalDateTime.now();
    }
}

