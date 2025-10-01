package com.example.Bookstore.service.order;

import com.example.Bookstore.domain.book.Book;
import com.example.Bookstore.domain.book.Inventory;
import com.example.Bookstore.domain.cart.Cart;
import com.example.Bookstore.domain.cart.CartItem;
import com.example.Bookstore.domain.cart.CartStatus;
import com.example.Bookstore.domain.order.Order;
import com.example.Bookstore.domain.order.OrderItem;
import com.example.Bookstore.domain.order.OrderStatus;
import com.example.Bookstore.domain.payment.Payment;
import com.example.Bookstore.domain.payment.PaymentMethod;
import com.example.Bookstore.repository.book.InventoryRepository;
import com.example.Bookstore.repository.cart.CartItemRepository;
import com.example.Bookstore.repository.cart.CartRepository;
import com.example.Bookstore.repository.order.OrderRepository;
import com.example.Bookstore.repository.payment.PaymentRepository;
import com.example.Bookstore.service.payment.PaymentService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final InventoryRepository inventoryRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    @Transactional(readOnly = true)
    public List<Order> getUserOrders(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Order getUserOrderDetail(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));
        if (!order.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Access denied to order: " + orderId);
        }
        return order;
    }

    @Transactional
    public Order placeOrderFromCart(Long userId, PaymentMethod method) {
        if (method != PaymentMethod.KAKAOPAY) {
            throw new IllegalArgumentException("Only KakaoPay is supported");
        }
        Cart cart = cartRepository.findByUser_IdAndStatus(userId, CartStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("Active cart not found for user: " + userId));

        List<CartItem> items = cart.getItems();
        if (items == null || items.isEmpty()) {
            throw new IllegalStateException("Cart is empty");
        }

        BigDecimal total = BigDecimal.ZERO;
        for (CartItem ci : items) {
            Book book = ci.getBook();
            Inventory inv = inventoryRepository.findByBook_Id(book.getId())
                    .orElseThrow(() -> new IllegalStateException("Inventory not found for book: " + book.getId()));
            long qty = ci.getQuantity() == null ? 0L : ci.getQuantity();
            if (qty <= 0) throw new IllegalStateException("Invalid quantity for book: " + book.getId());
            if (inv.getQuantity() < qty) {
                throw new IllegalStateException("Insufficient stock for book: " + book.getId());
            }
            total = total.add(book.getPrice().multiply(BigDecimal.valueOf(qty)));
        }

        Order order = Order.builder()
                .user(cart.getUser())
                .status(OrderStatus.PENDING)
                .totalAmount(total)
                .build();

        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem ci : items) {
            OrderItem oi = OrderItem.builder()
                    .order(order)
                    .book(ci.getBook())
                    .quantity(ci.getQuantity().intValue())
                    .unitPrice(ci.getBook().getPrice())
                    .build();
            orderItems.add(oi);
        }
        order.setItems(orderItems);

        order = orderRepository.save(order);

        Payment payment = paymentService.createPayment(order, method, total);
        paymentService.authorizeAndCapture(payment);

        order.setStatus(OrderStatus.PAID);
        for (CartItem ci : items) {
            Inventory inv = inventoryRepository.findByBook_Id(ci.getBook().getId())
                    .orElseThrow(() -> new IllegalStateException("Inventory not found for book: " + ci.getBook().getId()));
            inv.setQuantity(inv.getQuantity() - ci.getQuantity());
            inventoryRepository.save(inv);
        }

        cart.setStatus(CartStatus.CHECKED_OUT);
        cartItemRepository.deleteAll(new ArrayList<>(items));
        cart.getItems().clear();

        return order;
    }

    @Transactional
    public void cancelOrder(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));
        if (!order.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Access denied to order: " + orderId);
        }
        if (order.getStatus() == OrderStatus.CANCELLED) return;
        switch (order.getStatus()) {
            case PENDING, PAID -> {
                paymentRepository.findByOrderId(order.getId())
                        .ifPresent(paymentService::refund);
                for (OrderItem oi : order.getItems()) {
                    Inventory inv = inventoryRepository.findByBook_Id(oi.getBook().getId())
                            .orElseThrow(() -> new IllegalStateException("Inventory not found for book: " + oi.getBook().getId()));
                    inv.setQuantity(inv.getQuantity() + oi.getQuantity());
                    inventoryRepository.save(inv);
                }
                order.setStatus(OrderStatus.CANCELLED);
            }
            case SHIPPED, COMPLETED -> throw new IllegalStateException("Cannot cancel shipped/completed order");
            default -> {}
        }
    }

    @Transactional
    public Order adminUpdateOrderStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));
        order.setStatus(status);
        return order;
    }
}
