package com.example.Bookstore.repository.order;

import com.example.Bookstore.domain.order.Order;
import com.example.Bookstore.domain.order.OrderStatus;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OrderSpecifications {

    public static Specification<Order> statusEquals(OrderStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Order> userIdEquals(Long userId) {
        return (root, query, cb) -> userId == null ? null : cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<Order> createdBetween(LocalDateTime from, LocalDateTime to) {
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

    public static Specification<Order> totalBetween(BigDecimal min, BigDecimal max) {
        return (root, query, cb) -> {
            if (min == null && max == null) return null;
            if (min != null && max != null) {
                return cb.and(cb.greaterThanOrEqualTo(root.get("totalAmount"), min),
                              cb.lessThanOrEqualTo(root.get("totalAmount"), max));
            } else if (min != null) {
                return cb.greaterThanOrEqualTo(root.get("totalAmount"), min);
            } else {
                return cb.lessThanOrEqualTo(root.get("totalAmount"), max);
            }
        };
    }
}

