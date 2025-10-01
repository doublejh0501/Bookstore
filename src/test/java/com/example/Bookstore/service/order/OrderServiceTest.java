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
import com.example.Bookstore.domain.user.User;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentService paymentService;

    @InjectMocks private OrderService orderService;

    private User user;
    private Book book1;
    private Book book2;
    private Inventory inv1;
    private Inventory inv2;
    private Cart cart;
    private CartItem ci1;
    private CartItem ci2;

    @BeforeEach
    void setup() {
        user = User.builder().id(1L).email("a@a.com").password("pw").build();

        book1 = Book.builder().id(11L).isbn(1001L).title("B1").publisher("P")
                .price(new BigDecimal("10.00")).size("S").build();
        book2 = Book.builder().id(22L).isbn(1002L).title("B2").publisher("P")
                .price(new BigDecimal("20.00")).size("S").build();

        inv1 = Inventory.builder().id(101L).book(book1).quantity(10L).build();
        inv2 = Inventory.builder().id(202L).book(book2).quantity(5L).build();

        cart = Cart.builder()
                .id(1000L)
                .status(CartStatus.ACTIVE)
                .user(user)
                .items(new ArrayList<>())
                .build();

        ci1 = CartItem.builder().id(1L).cart(cart).book(book1).quantity(2L).build();
        ci2 = CartItem.builder().id(2L).cart(cart).book(book2).quantity(1L).build();
        cart.getItems().add(ci1);
        cart.getItems().add(ci2);
    }

    @Test
    @DisplayName("getUserOrders delegates to repository")
    void getUserOrders_success() {
        when(orderRepository.findByUserId(1L)).thenReturn(List.of());

        List<Order> result = orderService.getUserOrders(1L);

        assertNotNull(result);
        verify(orderRepository).findByUserId(1L);
    }

    @Test
    @DisplayName("getUserOrderDetail returns when user owns order")
    void getUserOrderDetail_success() {
        Order order = Order.builder().id(777L).user(user).status(OrderStatus.PENDING).build();
        when(orderRepository.findById(777L)).thenReturn(Optional.of(order));

        Order found = orderService.getUserOrderDetail(1L, 777L);

        assertEquals(777L, found.getId());
        verify(orderRepository).findById(777L);
    }

    @Test
    @DisplayName("getUserOrderDetail throws when different user")
    void getUserOrderDetail_differentUser_throws() {
        User other = User.builder().id(2L).email("b@b.com").password("pw").build();
        Order order = Order.builder().id(700L).user(other).status(OrderStatus.PENDING).build();
        when(orderRepository.findById(700L)).thenReturn(Optional.of(order));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> orderService.getUserOrderDetail(1L, 700L));
        assertTrue(ex.getMessage().contains("Access denied"));
    }

    @Test
    @DisplayName("placeOrderFromCart processes payment, updates stock, empties cart")
    void placeOrderFromCart_success() {
        when(cartRepository.findByUser_IdAndStatus(1L, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(inventoryRepository.findByBook_Id(11L)).thenReturn(Optional.of(inv1));
        when(inventoryRepository.findByBook_Id(22L)).thenReturn(Optional.of(inv2));

        ArgumentCaptor<Order> orderSaveCaptor = ArgumentCaptor.forClass(Order.class);
        when(orderRepository.save(orderSaveCaptor.capture())).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(9000L);
            return o;
        });

        Payment payment = Payment.builder().id(3000L).build();
        when(paymentService.createPayment(any(Order.class), eq(PaymentMethod.KAKAOPAY), any(BigDecimal.class)))
                .thenReturn(payment);
        when(paymentService.authorizeAndCapture(payment)).thenReturn(payment);

        Order placed = orderService.placeOrderFromCart(1L, PaymentMethod.KAKAOPAY);

        // total: 2*10 + 1*20 = 40
        Order savedOrder = orderSaveCaptor.getValue();
        assertEquals(new BigDecimal("40.00"), savedOrder.getTotalAmount());
        assertEquals(OrderStatus.PAID, placed.getStatus());

        // inventory decreased
        assertEquals(8L, inv1.getQuantity());
        assertEquals(4L, inv2.getQuantity());
        verify(inventoryRepository, times(2)).save(any(Inventory.class));

        // cart marked checked out and emptied
        assertEquals(CartStatus.CHECKED_OUT, cart.getStatus());
        verify(cartItemRepository).deleteAll(anyList());
        assertTrue(cart.getItems().isEmpty());

        // payment interactions
        verify(paymentService).createPayment(any(Order.class), eq(PaymentMethod.KAKAOPAY), eq(new BigDecimal("40.00")));
        verify(paymentService).authorizeAndCapture(payment);
    }

    @Test
    @DisplayName("placeOrderFromCart fails when empty cart")
    void placeOrderFromCart_emptyCart_throws() {
        Cart emptyCart = Cart.builder().id(2000L).status(CartStatus.ACTIVE).user(user).items(new ArrayList<>()).build();
        when(cartRepository.findByUser_IdAndStatus(1L, CartStatus.ACTIVE)).thenReturn(Optional.of(emptyCart));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> orderService.placeOrderFromCart(1L, PaymentMethod.KAKAOPAY));
        assertTrue(ex.getMessage().contains("Cart is empty"));
    }

    @Test
    @DisplayName("placeOrderFromCart fails when stock insufficient")
    void placeOrderFromCart_insufficientStock_throws() {
        inv1.setQuantity(1L); // need 2
        when(cartRepository.findByUser_IdAndStatus(1L, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(inventoryRepository.findByBook_Id(11L)).thenReturn(Optional.of(inv1));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> orderService.placeOrderFromCart(1L, PaymentMethod.KAKAOPAY));
        assertTrue(ex.getMessage().contains("Insufficient stock"));
    }



    @Test
    @DisplayName("cancelOrder refunds and restocks for PENDING/PAID")
    void cancelOrder_refundAndRestock() {
        Order order = Order.builder()
                .id(5000L)
                .user(user)
                .status(OrderStatus.PAID)
                .items(new ArrayList<>())
                .build();
        OrderItem oi1 = OrderItem.builder().order(order).book(book1).quantity(2).build();
        OrderItem oi2 = OrderItem.builder().order(order).book(book2).quantity(1).build();
        order.getItems().add(oi1);
        order.getItems().add(oi2);

        Payment payment = Payment.builder().id(333L).build();

        when(orderRepository.findById(5000L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(5000L)).thenReturn(Optional.of(payment));
        when(inventoryRepository.findByBook_Id(11L)).thenReturn(Optional.of(inv1));
        when(inventoryRepository.findByBook_Id(22L)).thenReturn(Optional.of(inv2));

        orderService.cancelOrder(1L, 5000L);

        // status updated
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        // refunded
        verify(paymentService).refund(payment);
        // restocked
        assertEquals(12L, inv1.getQuantity());
        assertEquals(6L, inv2.getQuantity());
        verify(inventoryRepository, times(2)).save(any(Inventory.class));
    }

    @Test
    @DisplayName("cancelOrder rejects when shipped or completed")
    void cancelOrder_rejectsShippedOrCompleted() {
        Order shipped = Order.builder().id(1L).user(user).status(OrderStatus.SHIPPED).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(shipped));
        assertThrows(IllegalStateException.class, () -> orderService.cancelOrder(1L, 1L));
    }

    @Test
    @DisplayName("get/cancel throws when order not found")
    void orderNotFound_throws() {
        when(orderRepository.findById(123L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> orderService.getUserOrderDetail(1L, 123L));
        assertThrows(EntityNotFoundException.class, () -> orderService.cancelOrder(1L, 123L));
    }

    @Test
    @DisplayName("adminUpdateOrderStatus updates status")
    void adminUpdateOrderStatus_success() {
        Order order = Order.builder().id(99L).user(user).status(OrderStatus.PENDING).build();
        when(orderRepository.findById(99L)).thenReturn(Optional.of(order));

        Order updated = orderService.adminUpdateOrderStatus(99L, OrderStatus.SHIPPED);
        assertEquals(OrderStatus.SHIPPED, updated.getStatus());
    }
}
