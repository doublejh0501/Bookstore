package com.example.Bookstore.controller.payment;

import com.example.Bookstore.config.KakaoPayProperties;
import com.example.Bookstore.domain.order.Order;
import com.example.Bookstore.domain.payment.Payment;
import com.example.Bookstore.repository.payment.PaymentRepository;
import com.example.Bookstore.service.order.OrderService;
import com.example.Bookstore.service.payment.kakao.KakaoPayClient;
import com.example.Bookstore.service.payment.kakao.KakaoPayClient.ApproveRequest;
import com.example.Bookstore.service.payment.kakao.KakaoPayClient.ApproveResponse;
import com.example.Bookstore.service.payment.kakao.KakaoPayClient.ReadyRequest;
import com.example.Bookstore.service.payment.kakao.KakaoPayClient.ReadyResponse;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@RequiredArgsConstructor
public class KakaoPayController {

  private final KakaoPayClient kakaoPayClient;
  private final OrderService orderService;
  private final PaymentRepository paymentRepository;
  private final KakaoPayProperties kakaoProps;

  /**
   * Create order from cart, create a PENDING payment, start KakaoPay and redirect.
   */
  @PostMapping("/payments/kakao/ready")
  @Transactional
  public RedirectView ready(@RequestParam("userId") Long userId) {
    Order order = orderService.prepareOrderFromCartForKakao(userId);
    BigDecimal total = order.getTotalAmount();

    ReadyRequest req = new ReadyRequest();
    req.setPartnerOrderId(String.valueOf(order.getId()));
    req.setPartnerUserId(String.valueOf(userId));
    req.setItemName("Bookstore 주문 " + order.getId());
    req.setQuantity(1);
    req.setTotalAmount(total.intValue());
    // Include orderId/userId in callback URLs to correlate
    String q = "?orderId=" + order.getId() + "&userId=" + userId;
    req.setApprovalUrl(kakaoProps.getApprovalUrl() + q);
    req.setCancelUrl(kakaoProps.getCancelUrl() + q);
    req.setFailUrl(kakaoProps.getFailUrl() + q);

    ReadyResponse res = kakaoPayClient.ready(req);

    // Save TID to payment
    paymentRepository.findByOrderId(order.getId()).ifPresent(p -> {
      p.setProviderTransactionId(res != null ? res.getTid() : null);
    });

    String redirect = res == null ? null : res.bestRedirectUrl();
    RedirectView rv = new RedirectView(redirect != null ? redirect : "/error");
    rv.setExposeModelAttributes(false);
    return rv;
  }

  @GetMapping("/payments/kakao/approve")
  @Transactional
  public ResponseEntity<ApproveResponse> approve(
      @RequestParam("pg_token") String pgToken,
      @RequestParam("orderId") Long orderId,
      @RequestParam("userId") Long userId
  ) {
    // Lookup TID from payment
    Payment payment = paymentRepository.findByOrderId(orderId).orElseThrow();
    String tid = payment.getProviderTransactionId();

    ApproveRequest req = new ApproveRequest();
    req.setTid(tid);
    req.setPartnerOrderId(String.valueOf(orderId));
    req.setPartnerUserId(String.valueOf(userId));
    req.setPgToken(pgToken);
    ApproveResponse res = kakaoPayClient.approve(req);

    // Finalize order and inventory on success
    orderService.finalizeOrderPaid(orderId);
    return ResponseEntity.ok(res);
  }

  @GetMapping("/payments/kakao/cancel")
  public ResponseEntity<String> cancel() {
    return ResponseEntity.ok("KakaoPay payment cancelled");
  }

  @GetMapping("/payments/kakao/fail")
  public ResponseEntity<String> fail() {
    return ResponseEntity.ok("KakaoPay payment failed");
  }
}
