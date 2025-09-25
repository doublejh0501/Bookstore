package com.example.Bookstore.repository.order;

import com.example.Bookstore.domain.order.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    List<OrderItem> findByBookId(Long bookId);

    Optional<OrderItem> findByOrderIdAndBookId(Long orderId, Long bookId);

    boolean existsByOrderIdAndBookId(Long orderId, Long bookId);
}

