package com.example.Bookstore.controller.admin;

import com.example.Bookstore.domain.order.Order;
import com.example.Bookstore.domain.order.OrderStatus;
import com.example.Bookstore.domain.payment.Payment;
import com.example.Bookstore.repository.order.OrderRepository;
import com.example.Bookstore.repository.payment.PaymentRepository;
import com.example.Bookstore.repository.order.OrderSpecifications;
import com.example.Bookstore.service.order.OrderService;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/orders")
public class AdminOrderController {

  private final OrderRepository orderRepository;
  private final PaymentRepository paymentRepository;
  private final OrderService orderService;

  public AdminOrderController(OrderRepository orderRepository,
      PaymentRepository paymentRepository,
      OrderService orderService) {
    this.orderRepository = Objects.requireNonNull(orderRepository);
    this.paymentRepository = Objects.requireNonNull(paymentRepository);
    this.orderService = Objects.requireNonNull(orderService);
  }

  @GetMapping
  public String list(
      @RequestParam(value = "status", required = false) String status,
      @RequestParam(value = "userId", required = false) String userId,
      @RequestParam(value = "from", required = false) String from,
      @RequestParam(value = "to", required = false) String to,
      @RequestParam(value = "minTotal", required = false) String minTotal,
      @RequestParam(value = "maxTotal", required = false) String maxTotal,
      @PageableDefault(size = 30) Pageable pageable,
      Model model) {
    Page<Order> page;
    LocalDate fromDate = null, toDate = null;
    try { if (from != null && !from.isBlank()) fromDate = LocalDate.parse(from); } catch (Exception ignore) {}
    try { if (to != null && !to.isBlank()) toDate = LocalDate.parse(to); } catch (Exception ignore) {}

    OrderStatus statusEnum = null;
    if (status != null && !status.isBlank()) {
      try { statusEnum = OrderStatus.valueOf(status); } catch (Exception ignore) {}
    }
    Long userIdVal = null;
    try { if (userId != null && !userId.isBlank()) userIdVal = Long.valueOf(userId); } catch (Exception ignore) {}
    BigDecimal minAmt = null, maxAmt = null;
    try { if (minTotal != null && !minTotal.isBlank()) minAmt = new BigDecimal(minTotal); } catch (Exception ignore) {}
    try { if (maxTotal != null && !maxTotal.isBlank()) maxAmt = new BigDecimal(maxTotal); } catch (Exception ignore) {}

    var spec = org.springframework.data.jpa.domain.Specification.where(OrderSpecifications.statusEquals(statusEnum))
        .and(OrderSpecifications.userIdEquals(userIdVal))
        .and(OrderSpecifications.createdBetween(
            fromDate != null ? fromDate.atStartOfDay() : null,
            toDate != null ? toDate.plusDays(1).atStartOfDay() : null
        ))
        .and(OrderSpecifications.totalBetween(minAmt, maxAmt));

    page = orderRepository.findAll(spec, pageable);
    List<OrderListItem> items = page.stream().map(this::toListItem).toList();
    model.addAttribute("items", items);
    model.addAttribute("page", page.getNumber());
    model.addAttribute("pageSize", page.getSize());
    model.addAttribute("totalPages", page.getTotalPages());
    // echo filters back
    model.addAttribute("status", status);
    model.addAttribute("userId", userId);
    model.addAttribute("from", from);
    model.addAttribute("to", to);
    model.addAttribute("minTotal", minTotal);
    model.addAttribute("maxTotal", maxTotal);
    return "admin/order-list";
  }

  @GetMapping("/{id}")
  public String detail(@PathVariable("id") Long id, Model model) {
    Order order = orderRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Order not found: " + id));

    var orderDto = new OrderDetail(
        order.getId(),
        order.getUser() != null ? order.getUser().getId() : null,
        order.getStatus(),
        order.getCreatedAt());

    List<OrderItemView> orderItems = order.getItems() == null ? List.of() : order.getItems().stream()
        .map(oi -> new OrderItemView(
            oi.getBook() != null ? oi.getBook().getTitle() : "-",
            oi.getQuantity() != null ? oi.getQuantity() : 0,
            oi.getUnitPrice() != null ? oi.getUnitPrice() : BigDecimal.ZERO,
            (oi.getUnitPrice() != null ? oi.getUnitPrice() : BigDecimal.ZERO)
                .multiply(BigDecimal.valueOf(oi.getQuantity() != null ? oi.getQuantity() : 0))
        ))
        .toList();

    Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
    var shipment = new ShipmentView(order.getUser() != null ? order.getUser().getAddress() : null);

    model.addAttribute("order", orderDto);
    model.addAttribute("orderItems", orderItems);
    model.addAttribute("payment", payment);
    model.addAttribute("shipment", shipment);
    return "admin/order-detail";
  }

  @PostMapping("/{id}/status")
  public String updateStatus(@PathVariable("id") Long id, @RequestParam("status") String status) {
    OrderStatus newStatus = OrderStatus.valueOf(status);
    orderService.adminUpdateOrderStatus(id, newStatus);
    return "redirect:/admin/orders/" + id;
  }

  private OrderListItem toListItem(Order o) {
    return new OrderListItem(
        o.getId(),
        o.getCreatedAt(),
        o.getUser() != null ? o.getUser().getId() : null,
        o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO,
        o.getStatus() != null ? o.getStatus().name() : "PENDING");
  }

  public record OrderListItem(Long id, LocalDateTime createdAt, Long memberId,
                              BigDecimal totalPrice, String status) {}

  public record OrderDetail(Long id, Long memberId, OrderStatus status, LocalDateTime createdAt) {}

  public record OrderItemView(String productName, int quantity, BigDecimal unitPrice, BigDecimal subtotal) {}

  public record ShipmentView(String address) {}
}
