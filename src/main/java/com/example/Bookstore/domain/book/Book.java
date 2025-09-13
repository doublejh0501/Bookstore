package com.example.Bookstore.domain.book;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "books", uniqueConstraints = {
        @UniqueConstraint(name = "uk_books_isbn", columnNames = {"isbn"})
})
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private Long isbn;

    @Column(nullable = false, length = 200)
    private String title;

    private String description;

    // Publisher name for search and display
    @Column(length = 120)
    private String publisher;

    private String imageUrl;

    private String previewUrl;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    // e.g., "148x210mm", or weight/size encoded as text
    @Column(length = 50)
    private String size;

    // Average or assigned rating; stored as decimal(3,2)
    @Column(precision = 3, scale = 2)
    private BigDecimal rating;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SaleStatus saleStatus;

    private LocalDate publishedDate;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private Long viewCnt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<BookAuthor> bookAuthors = new HashSet<>();

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (saleStatus == null) saleStatus = SaleStatus.ON_SALE;
    }
}
