package com.example.Bookstore.controller.user;

import com.example.Bookstore.domain.order.Order;
import com.example.Bookstore.domain.order.OrderStatus;
import com.example.Bookstore.domain.order.OrderItem;
import com.example.Bookstore.domain.book.Book;
import com.example.Bookstore.security.jwt.JwtPrincipal;
import com.example.Bookstore.service.order.OrderService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/mypage/orders")
public class MyOrderController {

  private final OrderService orderService;

  private static final Comparator<Order> CREATED_AT_DESC =
      Comparator.comparing(Order::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
          .reversed();

  public MyOrderController(OrderService orderService) {
    this.orderService = Objects.requireNonNull(orderService, "orderService 는 null 일 수 없습니다");
  }

  @GetMapping
  public String myOrders(@AuthenticationPrincipal JwtPrincipal principal, Model model) {
    if (principal == null) {
      return "redirect:/login";
    }

    List<OrderView> myOrders = orderService.getUserOrders(principal.userId()).stream()
        .sorted(CREATED_AT_DESC)
        .map(this::toView)
        .toList();

    model.addAttribute("myOrders", myOrders);
    return "user/my-orders";
  }

  private OrderView toView(Order order) {
    LocalDateTime createdAt = order.getCreatedAt();
    List<OrderItem> orderItems = order.getItems();
    if (orderItems == null) {
      orderItems = List.of();
    }
    List<OrderItemView> items = orderItems.stream()
        .map(this::toItemView)
        .collect(Collectors.toList());
    BigDecimal computedTotal = items.stream()
        .map(OrderItemView::lineTotal)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalAmount = order.getTotalAmount();
    if (totalAmount == null || totalAmount.compareTo(computedTotal) != 0) {
      totalAmount = computedTotal;
    }
    int totalQuantity = items.stream().mapToInt(OrderItemView::quantity).sum();
    return new OrderView(order.getId(), createdAt, order.getStatus(), totalAmount, totalQuantity, items);
  }

  private OrderItemView toItemView(OrderItem item) {
    Book book = item.getBook();
    String title = book != null ? book.getTitle() : "알 수 없는 도서";
    String imageUrl = book != null ? book.getImageUrl() : null;
    BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
    int quantity = item.getQuantity() != null ? item.getQuantity() : 0;
    BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
    Long bookId = book != null ? book.getId() : null;
    return new OrderItemView(item.getId(), bookId, title, imageUrl, quantity, unitPrice, lineTotal);
  }

  /**
   * 주문 리스트 화면에 필요한 최소 정보만 담은 뷰 모델입니다.
   */
  public record OrderView(
      Long id,
      LocalDateTime createdAt,
      OrderStatus status,
      BigDecimal totalAmount,
      int totalQuantity,
      List<OrderItemView> items) {
    public String statusLabel() {
      return status.getLabel();
    }
  }

  public record OrderItemView(
      Long id,
      Long bookId,
      String title,
      String imageUrl,
      int quantity,
      BigDecimal unitPrice,
      BigDecimal lineTotal) {
  }
}
