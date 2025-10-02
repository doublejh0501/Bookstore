package com.example.Bookstore.security.jwt; // JWT 관련 설정값을 보관할 패키지 선언

import jakarta.annotation.PostConstruct; // 설정 바인딩 이후 유효성 검증을 수행하기 위해 PostConstruct 사용
import java.nio.charset.StandardCharsets; // 시크릿 길이 검증을 위해 바이트 배열 길이를 손쉽게 얻기 위한 유틸
import java.time.Duration; // 토큰 만료 시간을 다룰 때 Duration 을 사용하면 편리합니다
import java.util.Collections; // 외부로 안전하게 읽기 전용 맵을 노출할 때 사용
import java.util.LinkedHashMap; // 입력 순서를 유지하면서 kid→secret 맵을 저장하기 위해 LinkedHashMap 사용
import java.util.Locale; // SameSite 값을 대소문자 구분 없이 처리하기 위해 Locale 사용
import java.util.Map; // kid→secret 맵과 같은 자료구조 표현
import java.util.Objects; // 값 비교 시 null 안전성을 확보하기 위해 사용
import java.util.Optional; // Optional 로 안전하게 값 추출
import java.util.concurrent.TimeUnit; // 만료 시간을 초→Duration 변환 시 사용
import org.springframework.boot.context.properties.ConfigurationProperties; // YML의 security.jwt.* 값을 바인딩하기 위한 어노테이션
import org.springframework.stereotype.Component; // Spring Bean 으로 등록해 다른 빈에서 주입 받을 수 있도록 지정
import org.springframework.util.StringUtils; // 문자열이 비어 있는지 쉽게 판별하기 위한 유틸

/**
 * JWT 관련 모든 설정을 typesafe 한 방식으로 관리하는 ConfigurationProperties 입니다.
 * 한 줄마다 상세한 설명을 담아 초심자도 쉽게 따라올 수 있도록 작성했습니다.
 */
@Component // 이 클래스를 구성 요소 스캔 대상(Spring Bean)으로 등록합니다
@ConfigurationProperties(prefix = "security.jwt") // application.yml 의 security.jwt.* 항목을 자동 바인딩합니다
public class JwtProperties {

  /** activeKid : 현재 서비스에서 서명에 사용할 kid 값 */
  private String activeKid = "primary"; // 기본 활성 kid 는 primary 로 둡니다

  /** secretMapRaw : "kid:secret" 형태를 쉼표로 이어붙인 원본 문자열 */
  private String secretMapRaw; // 환경변수에서 받은 원본 문자열을 그대로 보관합니다

  /** kidSecrets : 파싱된 kid→secret 맵 (입력 순서 유지) */
  private final Map<String, String> kidSecrets = new LinkedHashMap<>(); // 파싱된 결과를 저장합니다

  /** accessToken : 액세스 토큰 관련 하위 설정 */
  private final AccessTokenProperties accessToken = new AccessTokenProperties(); // 액세스 토큰 설정 객체

  /** refreshToken : 리프레시 토큰 관련 하위 설정 */
  private final RefreshTokenProperties refreshToken = new RefreshTokenProperties(); // 리프레시 토큰 설정 객체

  /**
   * activeKid 게터 - 다른 컴포넌트가 현재 활성화된 kid 를 확인할 때 사용합니다.
   */
  public String getActiveKid() { // activeKid 의 게터
    return activeKid; // 필드 값을 그대로 반환합니다
  }

  /**
   * activeKid 세터 - 설정 파일 값이 바인딩될 때 호출됩니다.
   */
  public void setActiveKid(String activeKid) { // activeKid 의 세터
    this.activeKid = Objects.requireNonNullElse(activeKid, "primary"); // null 인 경우 기본값 primary 로 대체합니다
  }

  /**
   * secretMapRaw 게터 - 단순 조회용입니다.
   */
  public String getSecretMapRaw() { // 원본 문자열 게터
    return secretMapRaw; // 그대로 반환합니다
  }

  /**
   * secretMapRaw 세터 - "kid:secret" 쌍을 파싱해 kidSecrets 맵에 채워 넣습니다.
   */
  public void setSecretMapRaw(String secretMapRaw) { // 원본 문자열 세터
    this.secretMapRaw = secretMapRaw; // 우선 필드에 저장합니다
    kidSecrets.clear(); // 기존 맵 내용을 초기화합니다(재기동 시 재파싱 대비)
    if (!StringUtils.hasText(secretMapRaw)) { // 값이 비어 있다면
      return; // 더 이상 진행할 수 없으므로 그대로 종료합니다
    }
    final String[] pairs = secretMapRaw.split(","); // 쉼표로 구분된 각 "kid:secret" 토큰을 분리합니다
    for (String pair : pairs) { // 각 토큰을 순회하면서
      final String trimmed = pair.trim(); // 앞뒤 공백을 제거합니다
      if (trimmed.isEmpty()) { // 빈 토큰은 건너뜁니다
        continue; // 다음 토큰으로 이동합니다
      }
      final String[] parts = trimmed.split(":", 2); // "kid:secret" 를 kid 와 secret 로 분리합니다
      if (parts.length != 2) { // 예상과 다른 형식이면
        throw new IllegalArgumentException("JWT secret format must be 'kid:secret'"); // 명확한 예외를 던집니다
      }
      final String kid = parts[0].trim(); // kid 부분 공백 제거
      final String secret = parts[1].trim(); // secret 부분 공백 제거
      if (!StringUtils.hasText(kid) || !StringUtils.hasText(secret)) { // 어느 한쪽이라도 비어 있다면
        throw new IllegalArgumentException("JWT secret entries require both kid and secret values"); // 상세 예외 메시지
      }
      validateSecretLength(secret); // secret 길이가 HS256 요구사항(32~64바이트)을 충족하는지 검사합니다
      kidSecrets.put(kid, secret); // 검증이 끝난 쌍을 맵에 추가합니다
    }
  }

  /**
   * kidSecrets 게터 - 외부에서는 수정 불가능한 읽기 전용 맵을 사용합니다.
   */
  public Map<String, String> getKidSecrets() { // secrets 맵 게터
    return Collections.unmodifiableMap(kidSecrets); // 방어적 읽기 전용 뷰로 감싸서 반환합니다
  }

  /**
   * accessToken 하위 설정 객체 게터
   */
  public AccessTokenProperties getAccessToken() { // 액세스 토큰 설정 게터
    return accessToken; // 구성 객체 자체를 반환합니다 (내부 필드는 별도의 세터를 통해 바인딩됨)
  }

  /**
   * refreshToken 하위 설정 객체 게터
   */
  public RefreshTokenProperties getRefreshToken() { // 리프레시 토큰 설정 게터
    return refreshToken; // 구성 객체 반환
  }

  /**
   * PostConstruct 훅에서 전체 설정 값의 정합성을 검증합니다.
   */
  @PostConstruct // 모든 값이 바인딩된 직후 검증 로직이 실행되도록 합니다
  public void validateAfterBinding() { // 유효성 검사 메서드
    if (kidSecrets.isEmpty()) { // 파싱된 시크릿 맵이 비었는지 확인
      throw new IllegalStateException("At least one JWT secret must be configured via JWT_SECRET environment variable"); // 명확한 오류 메시지
    }
    if (!kidSecrets.containsKey(activeKid)) { // 활성 kid 가 실제 맵에 존재하는지 확인
      throw new IllegalStateException("Active JWT kid '%s' is missing in configured secret map".formatted(activeKid)); // 자세한 메시지
    }
  }

  /**
   * 지정된 kid 에 해당하는 시크릿을 Optional 로 반환합니다.
   */
  public Optional<String> findSecretByKid(String kid) { // 특정 kid 에 대한 시크릿 조회 메서드
    return Optional.ofNullable(kidSecrets.get(kid)); // null 가능성을 Optional 로 감싸서 반환합니다
  }

  /**
   * 현재 활성 kid 에 해당하는 시크릿을 반환합니다.
   */
  public String getActiveSecret() { // 활성 시크릿 조회 메서드
    return kidSecrets.get(activeKid); // 활성 kid 를 키로 사용해 시크릿을 가져옵니다 (PostConstruct 검증 덕분에 null 아님)
  }

  /**
   * 액세스 토큰 만료 시간을 Duration 으로 편리하게 제공합니다.
   */
  public Duration getAccessTokenValidity() { // 액세스 토큰 만료 Duration 반환
    return Duration.ofSeconds(accessToken.expirySeconds); // 초 단위 설정값을 Duration 으로 변환합니다
  }

  /**
   * 리프레시 토큰 만료 시간을 Duration 으로 제공합니다.
   */
  public Duration getRefreshTokenValidity() { // 리프레시 토큰 만료 Duration 반환
    return Duration.ofSeconds(refreshToken.expirySeconds); // 초 단위 값을 Duration 으로 변환합니다
  }

  /**
   * HS256 요구사항(32~64바이트)을 만족하는지 검증합니다.
   */
  private void validateSecretLength(String secret) { // secret 길이 검증 메서드
    final int byteLength = secret.getBytes(StandardCharsets.UTF_8).length; // UTF-8 기준 바이트 길이를 구합니다
    if (byteLength < 32 || byteLength > 64) { // 32~64 바이트 범위를 벗어나면
      throw new IllegalArgumentException("HS256 secret must be between 32 and 64 bytes"); // 요구사항 위반 예외
    }
  }

  /** 액세스 토큰 관련 설정을 그룹핑한 내부 클래스 */
  public static class AccessTokenProperties { // 액세스 토큰 설정 보관 클래스

    /** 만료 시간(초) 기본값 600초 */
    private long expirySeconds = TimeUnit.MINUTES.toSeconds(10); // 기본 만료 시간을 10분으로 설정

    /** 쿠키 이름 기본값 BOOKSTORE_AT */
    private String cookieName = "BOOKSTORE_AT"; // HttpOnly 쿠키 이름

    /** 쿠키 경로 기본값 / */
    private String cookiePath = "/"; // 쿠키가 전송될 URL 경로

    /** Secure 플래그 기본값 true */
    private boolean cookieSecure = true; // HTTPS 전용 전송 여부

    /** HttpOnly 플래그 기본값 true */
    private boolean cookieHttpOnly = true; // JS 접근 차단 여부

    /** SameSite 속성 기본값 Lax */
    private String cookieSameSite = "Lax"; // SameSite 설정 문자열

    public long getExpirySeconds() { // 만료 시간 게터
      return expirySeconds; // 값 반환
    }

    public void setExpirySeconds(long expirySeconds) { // 만료 시간 세터
      this.expirySeconds = expirySeconds; // 단순 할당
    }

    public String getCookieName() { // 쿠키 이름 게터
      return cookieName; // 값 반환
    }

    public void setCookieName(String cookieName) { // 쿠키 이름 세터
      this.cookieName = cookieName; // 값 할당
    }

    public String getCookiePath() { // 쿠키 경로 게터
      return cookiePath; // 값 반환
    }

    public void setCookiePath(String cookiePath) { // 쿠키 경로 세터
      this.cookiePath = cookiePath; // 값 할당
    }

    public boolean isCookieSecure() { // Secure 여부 게터
      return cookieSecure; // 값 반환
    }

    public void setCookieSecure(boolean cookieSecure) { // Secure 여부 세터
      this.cookieSecure = cookieSecure; // 값 할당
    }

    public boolean isCookieHttpOnly() { // HttpOnly 여부 게터
      return cookieHttpOnly; // 값 반환
    }

    public void setCookieHttpOnly(boolean cookieHttpOnly) { // HttpOnly 여부 세터
      this.cookieHttpOnly = cookieHttpOnly; // 값 할당
    }

    public String getCookieSameSite() { // SameSite 문자열 게터
      return cookieSameSite; // 값 반환
    }

    public void setCookieSameSite(String cookieSameSite) { // SameSite 세터
      this.cookieSameSite = cookieSameSite; // 값 할당
    }

    /** SameSite 문자열을 HTTP 쿠키 규격에 맞는 표준 표기(Lax/Strict/None)로 변환합니다. */
    public String getNormalizedSameSite() { // SameSite 표준화 메서드
      if (!StringUtils.hasText(cookieSameSite)) { // 값이 비어 있으면 기본값 Lax 를 반환합니다
        return "Lax"; // 명시적인 기본값
      }
      final String normalized = cookieSameSite.trim().toLowerCase(Locale.ROOT); // 입력을 소문자로 통일합니다
      return switch (normalized) { // 허용 가능한 값에 대해 표준 표기를 결정합니다
        case "strict" -> "Strict"; // Strict 는 첫 글자만 대문자
        case "none" -> "None"; // None 도 동일하게 처리
        default -> "Lax"; // 그 외 값은 안전하게 Lax 로 강제합니다
      };
    }
  }

  /** 리프레시 토큰 관련 설정을 그룹핑한 내부 클래스 */
  public static class RefreshTokenProperties { // 리프레시 토큰 설정 클래스

    /** 만료 시간(초) 기본값 14일 */
    private long expirySeconds = TimeUnit.DAYS.toSeconds(14); // 기본 만료 시간을 14일로 설정

    /** Redis 네임스페이스 기본값 auth:refresh */
    private String redisNamespace = "auth:refresh"; // Redis 키 접두사

    /** 재사용 감지용 JTI 접두사 기본값 jwt:rt */
    private String jtiPrefix = "jwt:rt"; // JTI 키 접두사

    /** 쿠키 이름 기본값 BOOKSTORE_RT */
    private String cookieName = "BOOKSTORE_RT"; // 리프레시 토큰 쿠키 이름

    /** 쿠키 경로 기본값 / */
    private String cookiePath = "/"; // 리프레시 쿠키 유효 경로

    /** Secure 플래그 기본값 true */
    private boolean cookieSecure = true; // HTTPS 에서만 전송

    /** HttpOnly 플래그 기본값 true */
    private boolean cookieHttpOnly = true; // JS 접근 차단

    /** SameSite 속성 기본값 Strict */
    private String cookieSameSite = "Strict"; // CSRF 방지를 위해 Strict 로 설정

    public long getExpirySeconds() { // 만료 시간 게터
      return expirySeconds; // 값 반환
    }

    public void setExpirySeconds(long expirySeconds) { // 만료 시간 세터
      this.expirySeconds = expirySeconds; // 값 할당
    }

    public String getRedisNamespace() { // Redis 네임스페이스 게터
      return redisNamespace; // 값 반환
    }

    public void setRedisNamespace(String redisNamespace) { // Redis 네임스페이스 세터
      this.redisNamespace = redisNamespace; // 값 할당
    }

    public String getJtiPrefix() { // JTI 접두사 게터
      return jtiPrefix; // 값 반환
    }

    public void setJtiPrefix(String jtiPrefix) { // JTI 접두사 세터
      this.jtiPrefix = jtiPrefix; // 값 할당
    }

    public String getCookieName() { // 쿠키 이름 게터
      return cookieName;
    }

    public void setCookieName(String cookieName) { // 쿠키 이름 세터
      this.cookieName = cookieName;
    }

    public String getCookiePath() { // 쿠키 경로 게터
      return cookiePath;
    }

    public void setCookiePath(String cookiePath) { // 쿠키 경로 세터
      this.cookiePath = cookiePath;
    }

    public boolean isCookieSecure() { // Secure 플래그 게터
      return cookieSecure;
    }

    public void setCookieSecure(boolean cookieSecure) { // Secure 플래그 세터
      this.cookieSecure = cookieSecure;
    }

    public boolean isCookieHttpOnly() { // HttpOnly 플래그 게터
      return cookieHttpOnly;
    }

    public void setCookieHttpOnly(boolean cookieHttpOnly) { // HttpOnly 플래그 세터
      this.cookieHttpOnly = cookieHttpOnly;
    }

    public String getCookieSameSite() { // SameSite 문자열 게터
      return cookieSameSite;
    }

    public void setCookieSameSite(String cookieSameSite) { // SameSite 문자열 세터
      this.cookieSameSite = cookieSameSite;
    }

    /** SameSite 문자열을 RFC 표준 표기로 정규화합니다. */
    public String getNormalizedSameSite() {
      if (!StringUtils.hasText(cookieSameSite)) {
        return "Lax"; // 기본값 안전하게 Lax 로 설정
      }
      final String normalized = cookieSameSite.trim().toLowerCase(Locale.ROOT);
      return switch (normalized) {
        case "strict" -> "Strict";
        case "none" -> "None";
        default -> "Lax";
      };
    }
  }
}
