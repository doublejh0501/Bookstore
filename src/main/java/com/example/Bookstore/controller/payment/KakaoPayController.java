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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.util.StringUtils;
import jakarta.persistence.EntityNotFoundException;

@Controller
@RequiredArgsConstructor
@Slf4j
public class KakaoPayController {

  private final KakaoPayClient kakaoPayClient;
  private final OrderService orderService;
  private final PaymentRepository paymentRepository;
  private final KakaoPayProperties kakaoProps;

  /**
   * Create order from cart, create a PENDING payment, start KakaoPay and redirect.
   */
  @PostMapping("/payments/kakao/ready")
  public RedirectView ready(@RequestParam("userId") Long userId) {
    try {
      // Feature-flag: if disabled, inform user and return to checkout
      if (!kakaoProps.isEnabled()) {
        return buildRedirect("/checkout", "결제는 정식 배포 후 제공됩니다.");
      }

      // Guard: missing KakaoPay credentials
      if (!StringUtils.hasText(kakaoProps.getClientId()) || !StringUtils.hasText(kakaoProps.getSecretKey())) {
        log.warn("KakaoPay credentials missing. clientId or secretKey is blank");
        return buildRedirect(kakaoProps.getFailUrl(), "Missing KakaoPay credentials");
      }

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

      if (res == null || !StringUtils.hasText(res.getTid())) {
        log.warn("KakaoPay ready returned null or missing tid for orderId={}", order.getId());
        return buildRedirect(kakaoProps.getFailUrl(), "KakaoPay ready failed");
      }

      // Save TID to payment
      paymentRepository.findByOrderId(order.getId()).ifPresent(p -> {
        p.setProviderTransactionId(res.getTid());
        paymentRepository.save(p);
      });

      String redirect = res.bestRedirectUrl();
      return buildRedirect(redirect != null ? redirect : kakaoProps.getFailUrl(), null);
    } catch (EntityNotFoundException | IllegalArgumentException | IllegalStateException ex) {
      log.warn("KakaoPay ready validation failed: {}", ex.getMessage());
      // Common causes: empty cart, insufficient stock, invalid quantity
      return buildRedirect(kakaoProps.getFailUrl(), ex.getMessage());
    } catch (Exception ex) {
      log.error("KakaoPay ready error", ex);
      return buildRedirect(kakaoProps.getFailUrl(), "Unexpected error during KakaoPay ready");
    }
  }

  @GetMapping("/payments/kakao/approve")
  public ResponseEntity<ApproveResponse> approve(
      @RequestParam("pg_token") String pgToken,
      @RequestParam("orderId") Long orderId,
      @RequestParam("userId") Long userId
  ) {
    try {
      // Lookup TID from payment
      Payment payment = paymentRepository.findByOrderId(orderId).orElseThrow();
      String tid = payment.getProviderTransactionId();
      if (!StringUtils.hasText(tid)) {
        throw new IllegalStateException("Missing KakaoPay TID for orderId=" + orderId);
      }

      ApproveRequest req = new ApproveRequest();
      req.setTid(tid);
      req.setPartnerOrderId(String.valueOf(orderId));
      req.setPartnerUserId(String.valueOf(userId));
      req.setPgToken(pgToken);
      ApproveResponse res = kakaoPayClient.approve(req);

      // Finalize order and inventory on success
      orderService.finalizeOrderPaid(orderId);
      return ResponseEntity.ok(res);
    } catch (Exception ex) {
      log.error("KakaoPay approve error for orderId={}", orderId, ex);
      return ResponseEntity.internalServerError().build();
    }
  }

  @GetMapping("/payments/kakao/cancel")
  public ResponseEntity<String> cancel() {
    return ResponseEntity.ok("KakaoPay payment cancelled");
  }

  @GetMapping("/payments/kakao/fail")
  public ResponseEntity<String> fail() {
    return ResponseEntity.ok("KakaoPay payment failed");
  }

  // Helper to build redirect with optional message
  private RedirectView buildRedirect(String url, String message) {
    String dest = url;
    if (StringUtils.hasText(message)) {
      String sep = url.contains("?") ? "&" : "?";
      dest = url + sep + "reason=" + java.net.URLEncoder.encode(message, java.nio.charset.StandardCharsets.UTF_8);
    }
    RedirectView rv = new RedirectView(dest);
    rv.setExposeModelAttributes(false);
    return rv;
  }
}
