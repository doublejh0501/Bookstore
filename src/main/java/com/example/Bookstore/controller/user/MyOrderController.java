package com.example.Bookstore.controller.user;

import com.example.Bookstore.domain.order.Order;
import com.example.Bookstore.domain.order.OrderStatus;
import com.example.Bookstore.domain.order.OrderItem;
import com.example.Bookstore.domain.book.Book;
import com.example.Bookstore.security.jwt.JwtPrincipal;
import com.example.Bookstore.repository.payment.PaymentRepository;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/mypage/orders")
public class MyOrderController {

  private final OrderService orderService;
  private final PaymentRepository paymentRepository;

  private static final Comparator<Order> CREATED_AT_DESC =
      Comparator.comparing(Order::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
          .reversed();

  public MyOrderController(OrderService orderService, PaymentRepository paymentRepository) {
    this.orderService = Objects.requireNonNull(orderService, "orderService 는 null 일 수 없습니다");
    this.paymentRepository = Objects.requireNonNull(paymentRepository, "paymentRepository 는 null 일 수 없습니다");
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

  @GetMapping("/{id}")
  public String myOrderDetail(
      @PathVariable("id") Long orderId,
      @AuthenticationPrincipal JwtPrincipal principal,
      Model model) {
    if (principal == null) {
      return "redirect:/login";
    }
    Order order = orderService.getUserOrderDetail(principal.userId(), orderId);

    // Build item views
    List<OrderItemView> items = order.getItems() == null ? List.of() : order.getItems().stream()
        .map(this::toItemView)
        .toList();

    // Simple shipment model using user's address (no Shipment entity yet)
    record ShipmentView(String address) {}
    ShipmentView shipment = new ShipmentView(order.getUser() != null ? order.getUser().getAddress() : null);

    model.addAttribute("order", new OrderDetail(order.getId(), isCancelable(order)));
    model.addAttribute("orderStatus", order.getStatus());
    model.addAttribute("orderItems", items);
    model.addAttribute("shipment", shipment);
    model.addAttribute("payment", paymentRepository.findByOrderId(order.getId()).orElse(null));
    return "user/my-order-detail";
  }

  @PostMapping("/{id}/cancel")
  public String cancelOrder(
      @PathVariable("id") Long orderId,
      @AuthenticationPrincipal JwtPrincipal principal,
      org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
    if (principal == null) {
      return "redirect:/login";
    }
    orderService.cancelOrder(principal.userId(), orderId);
    ra.addFlashAttribute("notice", "주문이 취소되었습니다.");
    return "redirect:/mypage/orders";
  }

  private boolean isCancelable(Order order) {
    return order.getStatus() == OrderStatus.PENDING || order.getStatus() == OrderStatus.PAID;
  }

  public record OrderDetail(Long id, boolean cancelable) {}

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
