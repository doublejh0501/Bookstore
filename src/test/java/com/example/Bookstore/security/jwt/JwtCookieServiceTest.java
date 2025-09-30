package com.example.Bookstore.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;

class JwtCookieServiceTest {

  private final Clock fixedClock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC);

  @Test
  void writeAccessTokenShouldAttachCookieWithExpectedAttributes() {
    JwtProperties properties = new JwtProperties();
    properties.getAccessToken().setCookieName("TEST_AT");
    properties.getAccessToken().setCookiePath("/");
    properties.getAccessToken().setCookieSecure(false);
    properties.getAccessToken().setCookieHttpOnly(true);
    properties.getAccessToken().setCookieSameSite("Lax");

    JwtCookieService cookieService = new JwtCookieService(properties, fixedClock);
    MockHttpServletResponse response = new MockHttpServletResponse();

    cookieService.writeAccessToken(response, "token-value", Instant.parse("2024-01-01T00:10:00Z"));

    String setCookie = response.getHeader(HttpHeaders.SET_COOKIE);
    assertThat(setCookie)
        .as("액세스 토큰 쿠키가 설정되어야 합니다")
        .contains("TEST_AT=token-value")
        .contains("Path=/")
        .contains("HttpOnly")
        .contains("SameSite=Lax");
  }
}
