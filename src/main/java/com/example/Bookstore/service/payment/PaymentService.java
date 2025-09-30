package com.example.Bookstore.service.payment;

import com.example.Bookstore.domain.order.Order;
import com.example.Bookstore.domain.payment.Payment;
import com.example.Bookstore.domain.payment.PaymentMethod;
import com.example.Bookstore.domain.payment.PaymentStatus;
import com.example.Bookstore.repository.payment.PaymentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Transactional
    public Payment createPayment(Order order, PaymentMethod method, BigDecimal amount) {
        if (paymentRepository.existsByOrderId(order.getId())) {
            return paymentRepository.findByOrderId(order.getId()).orElseThrow();
        }
        Payment payment = Payment.builder()
                .order(order)
                .method(method)
                .status(PaymentStatus.PENDING)
                .amount(amount)
                .build();
        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment authorizeAndCapture(Payment payment) {
        if (payment.getAmount() == null || payment.getAmount().signum() <= 0) {
            payment.setStatus(PaymentStatus.FAILED);
            return payment;
        }
        payment.setStatus(PaymentStatus.AUTHORIZED);
        payment.setProviderTransactionId(UUID.randomUUID().toString());
        payment.setStatus(PaymentStatus.CAPTURED);
        return payment;
    }

    @Transactional
    public Payment refund(Payment payment) {
        if (payment.getStatus() == PaymentStatus.CAPTURED || payment.getStatus() == PaymentStatus.AUTHORIZED) {
            payment.setStatus(PaymentStatus.REFUNDED);
        }
        return payment;
    }

    @Transactional(readOnly = true)
    public Payment getPaymentByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found for order: " + orderId));
    }
}
