package com.example.Bookstore.repository.order;

import com.example.Bookstore.domain.order.Order;
import com.example.Bookstore.domain.order.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserId(Long userId);

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByUserIdAndStatus(Long userId, OrderStatus status);

    Optional<Order> findTopByUserIdOrderByCreatedAtDesc(Long userId);

    boolean existsByUserIdAndStatus(Long userId, OrderStatus status);
}

