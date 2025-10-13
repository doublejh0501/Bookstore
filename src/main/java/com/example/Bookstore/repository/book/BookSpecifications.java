package com.example.Bookstore.repository.book;

import com.example.Bookstore.domain.book.Book;
import com.example.Bookstore.domain.book.SaleStatus;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import java.time.LocalDateTime;

public class BookSpecifications {

    public static Specification<Book> titleOrPublisherContains(String q) {
        return (root, query, cb) -> {
            if (q == null || q.isBlank()) return null;
            String like = "%" + q.toLowerCase() + "%";
            var titleLike = cb.like(cb.lower(root.get("title")), like);
            var publisherLike = cb.like(cb.lower(root.get("publisher")), like);
            return cb.or(titleLike, publisherLike);
        };
    }

    public static Specification<Book> authorNameContains(String q) {
        return (root, query, cb) -> {
            if (q == null || q.isBlank()) return null;
            query.distinct(true);
            var ba = root.join("bookAuthors", JoinType.LEFT);
            var author = ba.join("author", JoinType.LEFT);
            return cb.like(cb.lower(author.get("name")), "%" + q.toLowerCase() + "%");
        };
    }

    public static Specification<Book> saleStatusEquals(SaleStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("saleStatus"), status);
    }

    public static Specification<Book> createdBetween(LocalDateTime from, LocalDateTime to) {
        return (root, query, cb) -> {
            if (from == null && to == null) return null;
            if (from != null && to != null) {
                return cb.between(root.get("createdAt"), from, to);
            } else if (from != null) {
                return cb.greaterThanOrEqualTo(root.get("createdAt"), from);
            } else {
                return cb.lessThan(root.get("createdAt"), to);
            }
        };
    }

    public static Specification<Book> stockBetween(Long min, Long max) {
        return (root, query, cb) -> {
            if (min == null && max == null) return null;
            var inv = root.join("inventory", JoinType.LEFT);
            Path<Long> qty = inv.get("quantity");
            Expression<Long> qtyExpr = cb.<Long>selectCase().when(cb.isNull(qty), 0L).otherwise(qty);
            if (min != null && max != null) {
                return cb.and(cb.ge(qtyExpr, min), cb.le(qtyExpr, max));
            } else if (min != null) {
                return cb.ge(qtyExpr, min);
            } else {
                return cb.le(qtyExpr, max);
            }
        };
    }
}
