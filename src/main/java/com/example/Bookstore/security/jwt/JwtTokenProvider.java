package com.example.Bookstore.security.jwt; // JWT 생성·검증 로직이 모이는 패키지

import com.fasterxml.jackson.core.type.TypeReference; // JWT 헤더의 kid 를 파싱할 때 사용
import com.fasterxml.jackson.databind.ObjectMapper; // JSON 파싱을 위해 사용
import io.jsonwebtoken.Claims; // JWT 클레임 표현 타입
import io.jsonwebtoken.Jws; // 서명 검증이 끝난 JWT 표현 타입
import io.jsonwebtoken.JwtBuilder; // JWT 빌더
import io.jsonwebtoken.JwtException; // jjwt 가 던지는 공통 예외
import io.jsonwebtoken.Jwts; // JWT 생성/파싱 진입점
import io.jsonwebtoken.SignatureAlgorithm; // HS256 선택에 사용
import io.jsonwebtoken.security.Keys; // HMAC 서명 키 생성 유틸
import java.io.IOException; // 헤더 파싱 실패 시 예외 처리
import java.nio.charset.StandardCharsets; // 문자열 → 바이트 전환 용도
import java.security.MessageDigest; // SHA-256 해시 계산에 사용
import java.security.NoSuchAlgorithmException; // SHA-256 미지원 시 예외 처리
import java.security.SecureRandom; // JTI 생성용 난수
import java.time.Clock; // 현재 시간을 테스트 친화적으로 주입하기 위해 사용
import java.time.Instant; // 토큰 발급/만료 시간을 표현
import java.time.temporal.ChronoUnit; // 만료 시간 계산 보조
import java.util.Arrays; // 문자열 권한 분해에 사용
import java.util.Base64; // JTI 및 헤더 디코딩에 사용
import java.util.Collection; // 권한 목록 표현
import java.util.Date; // jjwt 는 java.util.Date 를 사용하므로 변환 필요
import java.util.List; // 불변 리스트 반환에 사용
import java.util.Map; // 헤더 파싱 결과 표현
import java.util.Objects; // null 검증
import java.util.Optional; // Optional 활용으로 가독성 확보
import javax.crypto.SecretKey; // HMAC 서명용 키 타입
import org.springframework.beans.factory.annotation.Autowired; // 생성자 주입 명시
import org.springframework.stereotype.Component; // 스프링 빈 등록
import org.springframework.util.StringUtils; // 문자열 비어있음 검증

/**
 * HS256 JWT 발급·검증 책임을 모아둔 프로바이더입니다.
 * - access / refresh 토큰 모두 kid 헤더를 포함합니다.
 * - refresh 토큰 회전 시 Redis 화이트리스트를 호출합니다.
 * - HttpOnly 쿠키 전략을 염두에 두고 만료 Instant 를 함께 반환합니다.
 */
@Component // 다른 컴포넌트에서 손쉽게 주입해 사용할 수 있도록 빈으로 등록합니다
public class JwtTokenProvider {

  private static final int JTI_BYTE_LENGTH = 32; // 256비트 난수 → URL-safe 43자 내외
  private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder(); // JWT 헤더 파싱 시 사용
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper(); // 헤더 JSON 파싱용 공용 객체
  private static final TypeReference<Map<String, Object>> HEADER_TYPE = new TypeReference<>() {}; // 헤더 파싱 결과 타입

  private static final String HEADER_KID = "kid"; // kid 헤더 키
  private static final String CLAIM_TOKEN_TYPE = "token_type"; // 토큰 종류 구분용 클레임 키
  private static final String CLAIM_EMAIL = "email"; // 사용자 이메일 클레임 키
  private static final String CLAIM_NAME = "name"; // 사용자 표시 이름 클레임 키
  private static final String CLAIM_AUTHORITIES = "auth"; // 권한 목록 클레임 키
  private static final String CLAIM_DEVICE_ID = "device_id"; // 디바이스 식별자 클레임 키

  private final JwtProperties properties; // security.jwt.* 설정 값을 담은 빈
  private final RefreshTokenService refreshTokenService; // 리프레시 토큰 저장·회전 책임을 가진 추상화
  private final Clock clock; // 테스트 시 시계를 고정할 수 있도록 DI 로 주입
  private final SecureRandom secureRandom; // 암호학적으로 안전한 난수 생성기

  /**
   * 운영 코드에서 사용하는 기본 생성자. Clock 과 SecureRandom 은 표준 구현으로 주입합니다.
   */
  @Autowired
  public JwtTokenProvider(JwtProperties properties, RefreshTokenService refreshTokenService) {
    this(properties, refreshTokenService, Clock.systemUTC(), new SecureRandom()); // 테스트 가능성을 높이기 위해 오버로드 호출
  }

  /**
   * 테스트 전용 생성자. Clock/SecureRandom 을 교체할 수 있어 단위 테스트 작성이 수월해집니다.
   */
  public JwtTokenProvider(
      JwtProperties properties,
      RefreshTokenService refreshTokenService,
      Clock clock,
      SecureRandom secureRandom) {
    this.properties = properties; // 구성값 저장
    this.refreshTokenService = refreshTokenService; // 리프레시 토큰 서비스 주입
    this.clock = clock; // 현재 시각 계산용 Clock 저장
    this.secureRandom = secureRandom; // 난수 생성기 저장
  }

  /**
   * 로그인 성공 시 호출되어 액세스/리프레시 토큰을 한 번에 발급합니다.
   * refresh 토큰은 Redis 화이트리스트에 즉시 저장합니다.
   */
  public IssuedTokenPair issueTokens(JwtPrincipal principal) {
    Objects.requireNonNull(principal, "principal 은 null 일 수 없습니다"); // 방어적 null 체크

    final Instant issuedAt = now().truncatedTo(ChronoUnit.SECONDS); // JWT 표준에 맞춰 초 단위로 절삭
    final Instant accessExpiresAt = issuedAt.plus(properties.getAccessTokenValidity()); // 액세스 만료 계산
    final Instant refreshExpiresAt = issuedAt.plus(properties.getRefreshTokenValidity()); // 리프레시 만료 계산

    final String accessJti = generateJti(); // 액세스 토큰 JTI 생성
    final String refreshJti = generateJti(); // 리프레시 토큰 JTI 생성

    final String accessToken = buildSignedToken(principal, TokenType.ACCESS, accessJti, issuedAt, accessExpiresAt); // 액세스 토큰 생성
    final String refreshToken = buildSignedToken(principal, TokenType.REFRESH, refreshJti, issuedAt, refreshExpiresAt); // 리프레시 토큰 생성

    final String subject = principal.userId().toString(); // 리프레시 저장 시 사용할 subject (계정 식별자)
    final String hashedRefresh = hashRefreshToken(refreshToken); // 저장 용도로 해시 생성

    refreshTokenService.store( // 화이트리스트에 신규 리프레시 토큰 정보를 저장합니다
        new RefreshTokenService.StoreCommand(
            refreshJti,
            subject,
            principal.deviceId(),
            refreshExpiresAt,
            hashedRefresh));

    return new IssuedTokenPair(accessToken, refreshToken, accessExpiresAt, refreshExpiresAt); // 호출 측에 토큰 묶음을 반환합니다
  }

  /**
   * 요청 쿠키에서 꺼낸 액세스 토큰을 검증하고 클레임을 파싱해 돌려줍니다.
   */
  public JwtClaims parseAccessToken(String token) {
    Claims claims = parseAndValidate(token, TokenType.ACCESS); // 서명 및 만료 검증 후 Claims 확보
    return toJwtClaims(claims); // Claims → 우리 프로젝트 전용 DTO 로 변환
  }

  /**
   * 리프레시 토큰을 검증하고 클레임을 반환합니다. 로그아웃/재발급 시 공통으로 사용됩니다.
   */
  public JwtClaims parseRefreshToken(String token) {
    Claims claims = parseAndValidate(token, TokenType.REFRESH);
    return toJwtClaims(claims);
  }

  /**
   * 리프레시 토큰 재발급 요청이 들어왔을 때 토큰을 검증하고 회전 전략을 적용합니다.
   */
  public RefreshTokenRotationResult rotateRefreshToken(String refreshToken) {
    Claims claims = parseAndValidate(refreshToken, TokenType.REFRESH); // 리프레시 토큰 검증

    final Instant issuedAt = now().truncatedTo(ChronoUnit.SECONDS); // 새 토큰 발급 기준 시각
    final Instant newAccessExpires = issuedAt.plus(properties.getAccessTokenValidity()); // 새 액세스 토큰 만료
    final Instant newRefreshExpires = issuedAt.plus(properties.getRefreshTokenValidity()); // 새 리프레시 토큰 만료

    final String presentedHashedRefresh = hashRefreshToken(refreshToken); // 재사용 감지용 현재 토큰 해시

    final JwtPrincipal principal = rebuildPrincipalFrom(claims); // 리프레시 클레임 기반으로 사용자 컨텍스트 복원
    final String subject = principal.userId().toString(); // Redis 화이트리스트 키에 쓰일 subject

    final String previousJti = claims.getId(); // 기존 리프레시 토큰 JTI
    final String newRefreshJti = generateJti(); // 회전 후 리프레시 토큰 JTI
    final String newAccessJti = generateJti(); // 새 액세스 토큰 JTI

    final String newAccessToken = buildSignedToken(principal, TokenType.ACCESS, newAccessJti, issuedAt, newAccessExpires); // 새 액세스 토큰
    final String newRefreshToken = buildSignedToken(principal, TokenType.REFRESH, newRefreshJti, issuedAt, newRefreshExpires); // 새 리프레시 토큰

    final String newRefreshHash = hashRefreshToken(newRefreshToken); // 저장할 새 리프레시 토큰 해시

    RefreshTokenService.RotationStatus status = refreshTokenService.rotate(
        new RefreshTokenService.RotationCommand(
            previousJti,
            newRefreshJti,
            subject,
            principal.deviceId(),
            newRefreshExpires,
            presentedHashedRefresh,
            newRefreshHash));

    if (status.reuseDetected()) { // 재사용 감지 시 즉시 전체 세션을 폐기하고 예외를 던집니다
      refreshTokenService.invalidateFamily(subject, principal.deviceId()); // 동일 계정·기기 계열 토큰 전부 무효화
      throw new JwtAuthenticationException("Refresh token reuse detected for subject=" + subject); // 보안 경보 목적 메시지
    }

    return new RefreshTokenRotationResult(
        newAccessToken,
        newRefreshToken,
        newAccessExpires,
        newRefreshExpires,
        status.reuseDetected(),
        status.reusedJti()); // 호출 측에서 재사용 여부 로그를 남길 수 있도록 상태 정보를 함께 제공합니다
  }

  /**
   * 특정 리프레시 토큰 JTI 를 화이트리스트에서 제거(무효화)하기 위한 헬퍼입니다.
   * 로그아웃 시 사용하게 됩니다.
   */
  public void invalidateRefreshToken(String jti) {
    if (!StringUtils.hasText(jti)) { // JTI 가 비어 있다면 무시
      return;
    }
    refreshTokenService.invalidate(jti); // 실제 저장소 책임은 RefreshTokenService 가 맡습니다
  }

  /**
   * 반복적으로 필요한 JTI(토큰 식별자)를 생성하는 유틸리티 메서드입니다.
   */
  protected String generateJti() {
    final byte[] randomBytes = new byte[JTI_BYTE_LENGTH]; // 32바이트 버퍼 준비
    secureRandom.nextBytes(randomBytes); // 암호학적 난수 채우기
    return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes); // URL-safe 문자열로 변환하여 반환
  }

  /**
   * 현재 UTC 시간을 얻어오는 헬퍼입니다. Clock 을 주입받았기 때문에 테스트에서도 쉽게 제어할 수 있습니다.
   */
  protected Instant now() {
    return Instant.now(clock); // Clock 기준 현재 시각 반환
  }

  /**
   * 액세스/리프레시 토큰을 묶어서 전달하기 위한 레코드입니다.
   * 쿠키 설정 시 만료 시각도 함께 필요하므로 만료 Instant 를 포함해 둡니다.
   */
  public record IssuedTokenPair(
      String accessToken,
      String refreshToken,
      Instant accessTokenExpiresAt,
      Instant refreshTokenExpiresAt) {
  }

  /**
   * 리프레시 토큰 회전 시 호출 측에 되돌려 줄 정보를 담는 레코드입니다.
   * 재사용 감지 여부와 재사용된 JTI 도 포함해 추후 감사 로그에 활용할 수 있습니다.
   */
  public record RefreshTokenRotationResult(
      String newAccessToken,
      String newRefreshToken,
      Instant newAccessTokenExpiresAt,
      Instant newRefreshTokenExpiresAt,
      boolean reuseDetected,
      String reusedJti) {
  }

  /**
   * 클레임을 기반으로 JwtClaims DTO 를 구성합니다.
   */
  private JwtClaims toJwtClaims(Claims claims) {
    Collection<String> authorities = extractAuthorities(claims); // 권한 목록 추출
    String deviceId = claims.get(CLAIM_DEVICE_ID, String.class); // 디바이스 ID 추출
    Long userId = parseUserId(claims.getSubject()); // sub 는 사용자 ID 로 사용
    String email = claims.get(CLAIM_EMAIL, String.class); // 이메일 클레임
    if (!StringUtils.hasText(email)) {
      throw new JwtAuthenticationException("JWT claim 'email' is missing");
    }
    String displayName = claims.get(CLAIM_NAME, String.class); // 표시 이름은 null 허용
    return new JwtClaims(
        claims.getId(),
        userId,
        email,
        displayName,
        claims.getIssuer(),
        claims.getIssuedAt().toInstant(),
        claims.getExpiration().toInstant(),
        claims.getAudience(),
        authorities,
        deviceId);
  }

  /**
   * refresh 토큰 클레임에서 JwtPrincipal 을 복원합니다.
   */
  private JwtPrincipal rebuildPrincipalFrom(Claims claims) {
    Long userId = parseUserId(claims.getSubject()); // sub 를 Long 으로 변환
    String email = claims.get(CLAIM_EMAIL, String.class); // 이메일 클레임
    String displayName = claims.get(CLAIM_NAME, String.class); // 표시 이름(없을 수 있음)
    String deviceId = claims.get(CLAIM_DEVICE_ID, String.class); // 디바이스 식별자
    Collection<String> authorities = extractAuthorities(claims); // 권한 목록
    if (!StringUtils.hasText(email)) { // 이메일이 비어 있으면 보안상 허용하지 않습니다
      throw new JwtAuthenticationException("Refresh token is missing required email claim");
    }
    return new JwtPrincipal(userId, email, displayName, deviceId, authorities); // JwtPrincipal 재구성
  }

  /**
   * 문자열 subject 를 Long 으로 변환합니다.
   */
  private Long parseUserId(String subject) {
    try {
      return Long.valueOf(subject); // 숫자 변환
    } catch (NumberFormatException ex) {
      throw new JwtAuthenticationException("JWT subject is not a valid numeric user id: " + subject, ex); // 상세한 정보로 디버깅 도움
    }
  }

  /**
   * JWT 를 파싱하고 기대하는 토큰 타입(access/refresh)을 검증합니다.
   */
  private Claims parseAndValidate(String token, TokenType expectedType) {
    if (!StringUtils.hasText(token)) {
      throw new JwtAuthenticationException("JWT token is blank");
    }
    try {
      Claims claims = parseClaims(token); // 서명과 만료 검증
      String tokenType = claims.get(CLAIM_TOKEN_TYPE, String.class); // 토큰 타입 클레임 조회
      if (!expectedType.value.equals(tokenType)) { // 기대 타입과 다르면 거부
        throw new JwtAuthenticationException("JWT token_type mismatch. expected=" + expectedType.value + ", actual=" + tokenType);
      }
      return claims;
    } catch (JwtAuthenticationException ex) {
      throw ex; // 이미 감싼 예외는 그대로 전달
    } catch (JwtException | IllegalArgumentException ex) { // jjwt 가 던지는 예외 래핑
      throw new JwtAuthenticationException("JWT parsing failed", ex);
    }
  }

  /**
   * HS256 서명과 kid 를 이용해 Claims 를 추출합니다.
   */
  private Claims parseClaims(String token) {
    String kid = extractKid(token); // 헤더에서 kid 추출
    SecretKey key = resolveSigningKey(kid); // kid 에 해당하는 시크릿 키 확보
    Jws<Claims> jws = Jwts.parserBuilder()
        .setSigningKey(key)
        .build()
        .parseClaimsJws(token); // 서명, 만료 검증 및 Claims 추출
    return jws.getBody();
  }

  /**
   * JWT 헤더의 kid 값을 추출합니다.
   */
  private String extractKid(String token) {
    try {
      String[] parts = token.split("\\."); // JWT 는 header.payload.signature 3분절 구조
      if (parts.length != 3) {
        throw new JwtAuthenticationException("Malformed JWT token");
      }
      byte[] headerBytes = BASE64_URL_DECODER.decode(parts[0]); // URL-safe Base64 디코딩
      Map<String, Object> header = OBJECT_MAPPER.readValue(headerBytes, HEADER_TYPE); // JSON → Map 변환
      Object kidValue = header.get(HEADER_KID); // kid 추출
      if (kidValue instanceof String kid && StringUtils.hasText(kid)) {
        return kid;
      }
      throw new JwtAuthenticationException("JWT header missing kid");
    } catch (IllegalArgumentException | IOException ex) {
      throw new JwtAuthenticationException("Failed to parse JWT header", ex);
    }
  }

  /**
   * kid 에 해당하는 서명 키를 조회합니다.
   */
  private SecretKey resolveSigningKey(String kid) {
    Optional<String> secretOpt = properties.findSecretByKid(kid); // 구성된 시크릿 맵에서 조회
    String secret = secretOpt.orElseThrow(() -> new JwtAuthenticationException("Unknown JWT kid: " + kid)); // 없으면 즉시 예외
    return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); // HS256 키 생성
  }

  /**
   * JwtPrincipal 과 메타데이터를 받아 서명된 JWT 문자열을 생성합니다.
   */
  private String buildSignedToken(
      JwtPrincipal principal,
      TokenType tokenType,
      String jti,
      Instant issuedAt,
      Instant expiresAt) {
    SecretKey key = resolveSigningKey(properties.getActiveKid()); // 현재 활성 kid 로 서명

    JwtBuilder builder = Jwts.builder()
        .setHeaderParam(HEADER_KID, properties.getActiveKid()) // 헤더에 kid 명시
        .setSubject(principal.userId().toString()) // sub: 사용자 고유번호
        .setId(jti) // jti: 토큰 식별자
        .setIssuedAt(Date.from(issuedAt)) // iat: 발급 시각
        .setExpiration(Date.from(expiresAt)) // exp: 만료 시각
        .claim(CLAIM_TOKEN_TYPE, tokenType.value) // 토큰 용도 구분(access/refresh)
        .claim(CLAIM_EMAIL, principal.email()) // 이메일(로그 추적, 인증 컨텍스트 재구성용)
        .claim(CLAIM_AUTHORITIES, List.copyOf(principal.authorities())) // 권한 목록
        .claim(CLAIM_DEVICE_ID, principal.deviceId()); // 기기 식별자 (null 이면 claim 제거)

    if (StringUtils.hasText(principal.displayName())) { // 표시 이름이 있다면 클레임에 포함
      builder.claim(CLAIM_NAME, principal.displayName());
    }

    return builder
        .signWith(key, SignatureAlgorithm.HS256) // HS256 서명 적용
        .compact(); // 최종 JWT 문자열 생성
  }

  /**
   * SHA-256 해시를 Base64 문자열로 반환합니다. (Redis 에 저장 시 원본 노출을 피하기 위함)
   */
  private String hashRefreshToken(String token) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256"); // SHA-256 인스턴스 확보
      byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8)); // UTF-8 바이트 → 해시
      return Base64.getEncoder().encodeToString(hashed); // Base64 문자열로 변환
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 MessageDigest not available", ex); // 환경 문제는 런타임 예외로 전달
    }
  }

  /**
   * Claims 에서 권한 목록을 추출합니다.
   */
  private Collection<String> extractAuthorities(Claims claims) {
    Object raw = claims.get(CLAIM_AUTHORITIES); // auth 클레임 값 꺼내기
    if (raw instanceof Collection<?> collection) { // 리스트 형태로 들어온 경우
      return collection.stream().map(String::valueOf).toList(); // 문자열로 변환하여 불변 리스트 반환
    }
    if (raw instanceof String single) { // 콤마 문자열 형태로 들어온 경우 (호환성 대비)
      return Arrays.stream(single.split(","))
          .map(String::trim)
          .filter(StringUtils::hasText)
          .toList();
    }
    return List.of(); // 값이 없거나 예상과 다르면 빈 리스트 반환
  }

  /**
   * access/refresh 용도를 표현하는 내부 enum 입니다.
   */
  private enum TokenType {
    ACCESS("access"),
    REFRESH("refresh");

    private final String value; // 클레임에 저장될 실제 문자열 값

    TokenType(String value) {
      this.value = value;
    }
  }
}
