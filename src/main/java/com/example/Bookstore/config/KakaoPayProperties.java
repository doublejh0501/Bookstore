package com.example.Bookstore.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "kakaopay")
public class KakaoPayProperties {

  /** Merchant client identifier (a.k.a. CID for KakaoPay). */
  private String clientId;

  /** Secret key used for API authorization. */
  private String secretKey;

  /** KakaoPay CID value; TC0ONETIME for test by default. */
  private String cid = "TC0ONETIME";

  /** API base URL. */
  private String apiBase = "https://open-api.kakaopay.com";

  /** Callback URLs. */
  private String approvalUrl = "http://localhost:8080/payments/kakao/approve";
  private String cancelUrl = "http://localhost:8080/payments/kakao/cancel";
  private String failUrl = "http://localhost:8080/payments/kakao/fail";

  /** Feature flag: enable/disable actual payment calls. Default disabled for dev. */
  private boolean enabled = false;

  public String getClientId() { return clientId; }
  public void setClientId(String clientId) { this.clientId = clientId; }

  public String getSecretKey() { return secretKey; }
  public void setSecretKey(String secretKey) { this.secretKey = secretKey; }

  public String getCid() { return cid; }
  public void setCid(String cid) { this.cid = cid; }

  public String getApiBase() { return apiBase; }
  public void setApiBase(String apiBase) { this.apiBase = apiBase; }

  public String getApprovalUrl() { return approvalUrl; }
  public void setApprovalUrl(String approvalUrl) { this.approvalUrl = approvalUrl; }

  public String getCancelUrl() { return cancelUrl; }
  public void setCancelUrl(String cancelUrl) { this.cancelUrl = cancelUrl; }

  public String getFailUrl() { return failUrl; }
  public void setFailUrl(String failUrl) { this.failUrl = failUrl; }

  public boolean isEnabled() { return enabled; }
  public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
