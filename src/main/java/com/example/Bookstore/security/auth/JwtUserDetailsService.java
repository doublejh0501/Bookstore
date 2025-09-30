package com.example.Bookstore.security.auth; // 이메일 기반 사용자 조회를 담당하는 UserDetailsService 구현

import com.example.Bookstore.domain.user.User; // 사용자 엔티티
import com.example.Bookstore.repository.user.UserRepository; // 사용자 조회용 리포지토리
import java.util.Locale; // 에러 메시지에 이메일을 소문자로 표준화하기 위해 사용
import java.util.Objects; // null 검증
import lombok.RequiredArgsConstructor; // 생성자 주입을 간결하게 하기 위해 롬복 사용
import org.springframework.security.core.userdetails.UserDetails; // 반환 타입
import org.springframework.security.core.userdetails.UserDetailsService; // 인터페이스 구현
import org.springframework.security.core.userdetails.UsernameNotFoundException; // 사용자 미존재 예외
import org.springframework.stereotype.Service; // 스프링 빈 등록
import org.springframework.transaction.annotation.Transactional; // 읽기 전용 트랜잭션 보장

/**
 * 이메일을 username 으로 사용해 User 엔티티를 조회하고 {@link JwtUserDetails} 로 래핑하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
public class JwtUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository; // 사용자 조회 리포지토리

  @Override
  @Transactional(readOnly = true)
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    Objects.requireNonNull(username, "username 은 null 일 수 없습니다");
    User user = userRepository.findByEmail(username)
        .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username.toLowerCase(Locale.ROOT)));
    return JwtUserDetails.from(user);
  }
}
