package com.example.Bookstore.repository.payment;

import com.example.Bookstore.domain.payment.Payment;
import com.example.Bookstore.domain.payment.PaymentMethod;
import com.example.Bookstore.domain.payment.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(Long orderId);

    boolean existsByOrderId(Long orderId);

    List<Payment> findByStatus(PaymentStatus status);

    List<Payment> findByMethod(PaymentMethod method);

    Optional<Payment> findByProviderTransactionId(String providerTransactionId);

    List<Payment> findByOrderUser_Id(Long userId);

    List<Payment> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
}

