package com.example.Bookstore.repository.order;

import com.example.Bookstore.domain.order.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    List<OrderItem> findByBookId(Long bookId);

    Optional<OrderItem> findByOrderIdAndBookId(Long orderId, Long bookId);

    boolean existsByOrderIdAndBookId(Long orderId, Long bookId);

    @Query("""
            SELECT oi.book as book, sum(oi.quantity) as totalQty
            FROM OrderItem oi
            JOIN oi.order o
            WHERE o.status IN ('PAID', 'SHIPPED', 'COMPLETED')
            AND o.createdAt BETWEEN :start AND :end
            GROUP BY oi.book
            ORDER BY SUM(oi.quantity) DESC
            """)
    List<Object[]> findMonthlyBestsellers(@Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end);
}


