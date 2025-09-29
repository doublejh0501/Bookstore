package com.example.Bookstore.security.jwt; // Redis 기반 리프레시 토큰 화이트리스트 구현

import java.time.Clock; // TTL 계산 시 동일 기준 시각을 사용하기 위해 Clock 주입
import java.time.Duration; // TTL 설정 시 사용
import java.time.Instant; // 만료 시각 비교에 사용
import java.util.Objects; // null 체크를 간결하게 처리하기 위해 사용
import org.springframework.beans.factory.annotation.Autowired; // 생성자 주입 명시
import org.springframework.dao.DataAccessException; // Redis 접근 실패를 감지하기 위해 사용
import org.springframework.data.redis.core.StringRedisTemplate; // 문자열 기반 Redis 연산을 수행
import org.springframework.stereotype.Service; // 스프링 빈 등록
import org.springframework.util.StringUtils; // 문자열 공백 여부 판별

/**
 * Redis 를 이용해 리프레시 토큰을 화이트리스트 방식으로 관리하는 구현체입니다.
 * <p>
 * 저장소 구조 개요
 * <ul>
 *   <li><code>{namespace}:jti:{jti}</code> → 해시된 리프레시 토큰 (TTL = 만료 시간)</li>
 *   <li><code>{namespace}:lookup:{jti}</code> → 토큰이 속한 패밀리 키 (cleanup 용도)</li>
 *   <li><code>{namespace}:family:{subject}:{device}</code> → jti 세트 (기기별 전체 폐기를 위해 사용)</li>
 * </ul>
 */
@Service
public class RedisRefreshTokenService implements RefreshTokenService {

  private final StringRedisTemplate redisTemplate; // Redis 연산을 담당하는 템플릿
  private final JwtProperties properties; // 네임스페이스 등 구성 값을 제공
  private final Clock clock; // 만료 시간 계산 기준 시각

  @Autowired
  public RedisRefreshTokenService(StringRedisTemplate redisTemplate, JwtProperties properties) {
    this(redisTemplate, properties, Clock.systemUTC());
  }

  public RedisRefreshTokenService(StringRedisTemplate redisTemplate, JwtProperties properties, Clock clock) {
    this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate 는 null 일 수 없습니다");
    this.properties = Objects.requireNonNull(properties, "properties 는 null 일 수 없습니다");
    this.clock = Objects.requireNonNull(clock, "clock 는 null 일 수 없습니다");
  }

  @Override
  public void store(StoreCommand command) {
    Duration ttl = remainingTtl(command.expiresAt()); // 남은 TTL 계산
    String familyKey = familyKey(command.subject(), command.deviceId()); // 해당 토큰이 속한 패밀리 키
    execute(() -> {
      redisTemplate.opsForValue().set(jtiKey(command.jti()), command.hashedToken(), ttl); // 해시 저장
      redisTemplate.opsForValue().set(lookupKey(command.jti()), familyKey, ttl); // 역조회용 키 저장
      redisTemplate.opsForSet().add(familyKey, command.jti()); // 패밀리 세트에 JTI 추가
      redisTemplate.expire(familyKey, ttl); // 세트 TTL 동기화
      return null;
    });
  }

  @Override
  public RotationStatus rotate(RotationCommand command) {
    Duration ttl = remainingTtl(command.newExpiresAt()); // 새 토큰 TTL
    String previousKey = jtiKey(command.previousJti());
    String previousLookupKey = lookupKey(command.previousJti());
    String familyKey = familyKey(command.subject(), command.deviceId());

    return execute(() -> {
      String storedHash = redisTemplate.opsForValue().get(previousKey); // 기존 해시 조회
      if (!StringUtils.hasText(storedHash)) { // 존재하지 않으면 재사용 시나리오로 판단
        return new RotationStatus(true, command.previousJti());
      }
      if (!storedHash.equals(command.presentedHashedToken())) { // 저장된 해시와 불일치하면 재사용 감지
        return new RotationStatus(true, command.previousJti());
      }

      // 정상 회전: 이전 토큰 제거 후 새 토큰 저장
      redisTemplate.delete(previousKey); // 기존 토큰 제거
      redisTemplate.delete(previousLookupKey); // 역조회 키 제거
      redisTemplate.opsForSet().remove(familyKey, command.previousJti()); // 패밀리 세트에서도 제거

      redisTemplate.opsForValue().set(jtiKey(command.newJti()), command.newHashedToken(), ttl); // 새 토큰 저장
      redisTemplate.opsForValue().set(lookupKey(command.newJti()), familyKey, ttl); // 새 역조회 키 저장
      redisTemplate.opsForSet().add(familyKey, command.newJti()); // 패밀리 세트 업데이트
      redisTemplate.expire(familyKey, ttl); // 세트 TTL 갱신

      return new RotationStatus(false, null); // 정상 회전 결과 반환
    });
  }

  @Override
  public void invalidate(String jti) {
    if (!StringUtils.hasText(jti)) {
      return; // 비어 있는 입력은 무시
    }
    execute(() -> {
      String lookup = redisTemplate.opsForValue().get(lookupKey(jti)); // 패밀리 키 역조회
      redisTemplate.delete(jtiKey(jti)); // JTI 키 제거
      redisTemplate.delete(lookupKey(jti)); // 역조회 키 제거
      if (StringUtils.hasText(lookup)) {
        redisTemplate.opsForSet().remove(lookup, jti); // 패밀리 세트에서 제거
      }
      return null;
    });
  }

  @Override
  public void invalidateFamily(String subject, String deviceId) {
    String familyKey = familyKey(subject, deviceId);
    execute(() -> {
      var jtis = redisTemplate.opsForSet().members(familyKey); // 해당 기기에 속한 모든 JTI 조회
      if (jtis != null && !jtis.isEmpty()) {
        jtis.forEach(jti -> {
          redisTemplate.delete(jtiKey(jti));
          redisTemplate.delete(lookupKey(jti));
        });
      }
      redisTemplate.delete(familyKey); // 패밀리 세트 자체 삭제
      return null;
    });
  }

  private Duration remainingTtl(Instant expiresAt) {
    Instant now = Instant.now(clock);
    if (expiresAt.isBefore(now)) {
      return Duration.ZERO;
    }
    return Duration.between(now, expiresAt);
  }

  private String familyKey(String subject, String deviceId) {
    String deviceSegment = StringUtils.hasText(deviceId) ? deviceId : "default"; // deviceId 가 없으면 기본값 사용
    return namespacePrefix() + ":family:" + subject + ":" + deviceSegment;
  }

  private String jtiKey(String jti) {
    return namespacePrefix() + ":" + properties.getRefreshToken().getJtiPrefix() + ":" + jti;
  }

  private String lookupKey(String jti) {
    return namespacePrefix() + ":lookup:" + jti;
  }

  private String namespacePrefix() {
    return properties.getRefreshToken().getRedisNamespace();
  }

  private <T> T execute(RedisCallback<T> callback) {
    try {
      return callback.doInRedis();
    } catch (DataAccessException ex) {
      throw new JwtAuthenticationException("Redis access failed", ex);
    }
  }

  @FunctionalInterface
  private interface RedisCallback<T> {
    T doInRedis();
  }
}
