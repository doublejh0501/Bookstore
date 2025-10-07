package com.example.Bookstore.service.payment.kakao;

import com.example.Bookstore.config.KakaoPayProperties;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class KakaoPayClient {

  private final KakaoPayProperties props;
  private final RestTemplate restTemplate = new RestTemplate();

  public ReadyResponse ready(ReadyRequest req) {
    String base = props.getApiBase();
    String readyPath = (base != null && base.contains("kapi.kakao.com"))
        ? "/v1/payment/ready"
        : "/online/v1/payment/ready";
    String url = UriComponentsBuilder.fromHttpUrl(base)
        .path(readyPath)
        .toUriString();

    HttpHeaders headers = authHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("cid", defaultCid());
    form.add("partner_order_id", req.getPartnerOrderId());
    form.add("partner_user_id", req.getPartnerUserId());
    form.add("item_name", req.getItemName());
    form.add("quantity", String.valueOf(req.getQuantity()));
    form.add("total_amount", String.valueOf(req.getTotalAmount()));
    form.add("tax_free_amount", String.valueOf(req.getTaxFreeAmount()));
    String approvalUrl = req.getApprovalUrl() != null && !req.getApprovalUrl().isBlank()
        ? req.getApprovalUrl() : props.getApprovalUrl();
    String cancelUrl = req.getCancelUrl() != null && !req.getCancelUrl().isBlank()
        ? req.getCancelUrl() : props.getCancelUrl();
    String failUrl = req.getFailUrl() != null && !req.getFailUrl().isBlank()
        ? req.getFailUrl() : props.getFailUrl();
    form.add("approval_url", approvalUrl);
    form.add("cancel_url", cancelUrl);
    form.add("fail_url", failUrl);

    HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);
    return restTemplate.postForObject(URI.create(url), entity, ReadyResponse.class);
  }

  public ApproveResponse approve(ApproveRequest req) {
    String base = props.getApiBase();
    String approvePath = (base != null && base.contains("kapi.kakao.com"))
        ? "/v1/payment/approve"
        : "/online/v1/payment/approve";
    String url = UriComponentsBuilder.fromHttpUrl(base)
        .path(approvePath)
        .toUriString();

    HttpHeaders headers = authHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("cid", defaultCid());
    form.add("tid", req.getTid());
    form.add("partner_order_id", req.getPartnerOrderId());
    form.add("partner_user_id", req.getPartnerUserId());
    form.add("pg_token", req.getPgToken());

    HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);
    return restTemplate.postForObject(URI.create(url), entity, ApproveResponse.class);
  }

  private HttpHeaders authHeaders() {
    HttpHeaders headers = new HttpHeaders();
    // Choose header scheme by API base:
    // - open-api.kakaopay.com → Authorization: SECRET_KEY <secret>, KA-CLIENT-ID: <clientId>
    // - kapi.kakao.com        → Authorization: KakaoAK <admin_key>
    String base = props.getApiBase() == null ? "" : props.getApiBase();
    if (base.contains("kapi.kakao.com")) {
      headers.set("Authorization", "KakaoAK " + props.getSecretKey());
    } else {
      headers.set("Authorization", "SECRET_KEY " + props.getSecretKey());
      if (props.getClientId() != null && !props.getClientId().isBlank()) {
        headers.set("KA-CLIENT-ID", props.getClientId());
      }
    }
    return headers;
  }

  private String defaultCid() {
    return props.getCid() == null || props.getCid().isBlank() ? "TC0ONETIME" : props.getCid();
  }

  @Data
  public static class ReadyRequest {
    private String partnerOrderId;
    private String partnerUserId;
    private String itemName;
    private int quantity = 1;
    private int totalAmount;
    private int taxFreeAmount = 0;
    private String approvalUrl;
    private String cancelUrl;
    private String failUrl;
  }

  @Data
  public static class ReadyResponse {
    private String tid;
    private String next_redirect_app_url;
    private String next_redirect_mobile_url;
    private String next_redirect_pc_url;
    private String created_at;

    public String bestRedirectUrl() {
      // For desktop by default
      if (next_redirect_pc_url != null) return next_redirect_pc_url;
      if (next_redirect_mobile_url != null) return next_redirect_mobile_url;
      if (next_redirect_app_url != null) return next_redirect_app_url;
      return null;
    }
  }

  @Data
  public static class ApproveRequest {
    private String tid;
    private String partnerOrderId;
    private String partnerUserId;
    private String pgToken;
  }

  @Data
  public static class ApproveResponse {
    private String aid;
    private String tid;
    private String cid;
    private String sid;
    private String partner_order_id;
    private String partner_user_id;
    private String payment_method_type;
    private Map<String, Object> amount = new HashMap<>();
    private String item_name;
    private Integer quantity;
    private String created_at;
    private String approved_at;
  }
}
