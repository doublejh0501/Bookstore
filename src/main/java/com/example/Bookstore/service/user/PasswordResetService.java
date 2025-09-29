package com.example.Bookstore.service.user;

import com.example.Bookstore.config.AppMailProperties;
import com.example.Bookstore.config.PasswordResetProperties;
import com.example.Bookstore.domain.user.PasswordResetToken;
import com.example.Bookstore.domain.user.User;
import com.example.Bookstore.dto.auth.PasswordResetForm;
import com.example.Bookstore.repository.user.PasswordResetTokenRepository;
import com.example.Bookstore.repository.user.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 비밀번호 재설정 토큰 발급부터 실제 비밀번호 변경까지의 전 과정을 담당하는 서비스입니다.
 * <p>
 * 컨트롤러는 이 서비스를 호출해 이메일 발송 여부만 제어하고, 토큰 관리 · 보안 검증 ·
 * 비밀번호 인코딩 같은 핵심 로직은 모두 여기에서 수행합니다.
 */
@Service
@RequiredArgsConstructor
public class PasswordResetService {

  /**
   * 메일 본문에서 토큰 만료 시각을 보기 좋게 표현하기 위한 날짜/시간 포맷터입니다.
   */
  private static final DateTimeFormatter MAIL_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.KOREA);

  /** 사용자 엔티티를 이메일로 조회하기 위한 JPA 리포지토리입니다. */
  private final UserRepository userRepository;
  /** 토큰을 저장 · 조회 · 삭제하기 위한 리포지토리입니다. */
  private final PasswordResetTokenRepository passwordResetTokenRepository;
  /** 실제로 SMTP 서버와 통신하여 메일을 전송하는 스프링 컴포넌트입니다. */
  private final JavaMailSender mailSender;
  /** 발신자 주소 등 메일 관련 커스텀 설정 값을 담는 클래스입니다. */
  private final AppMailProperties mailProperties;
  /** 재설정 URL, 토큰 만료 시간 등 비밀번호 찾기 전용 설정을 담습니다. */
  private final PasswordResetProperties passwordResetProperties;
  /** 새 비밀번호를 해시(암호화)하기 위한 스프링 Security 컴포넌트입니다. */
  private final PasswordEncoder passwordEncoder;
  /** 현재 시간을 얻기 위한 {@link Clock}. 테스트에서 고정된 시간을 주입할 수 있게 합니다. */
  private final Clock clock;

  /**
   * 사용자가 이메일을 입력해 비밀번호 재설정을 요청했을 때 호출됩니다.
   * <ol>
   *   <li>이메일이 비어있지 않은지 검사하고,</li>
   *   <li>계정이 존재하면 이전 토큰을 정리한 뒤 새 토큰을 만들어 저장하고,</li>
   *   <li>안내 메일을 발송합니다.</li>
   * </ol>
   * 존재하지 않는 이메일은 조용히 무시하여 공격자가 계정 존재 여부를 추측하기 어렵게 합니다.
   */
  @Transactional
  public void sendResetLink(String rawEmail) {
    if (!StringUtils.hasText(rawEmail)) {
      // 입력이 비어 있으면 아무 작업도 하지 않습니다.
      return;
    }

    final String email = rawEmail.trim().toLowerCase(Locale.ROOT);
    final Optional<User> optionalUser = userRepository.findByEmail(email);
    if (optionalUser.isEmpty()) {
      // 존재하지 않는 이메일에 대해서도 성공처럼 보이게 해야 보안상 안전합니다.
      return;
    }

    LocalDateTime now = LocalDateTime.now(clock);
    User user = optionalUser.get();

    // 현재 시각 기준으로 만료된 토큰과 해당 사용자의 기존 토큰을 먼저 정리합니다.
    passwordResetTokenRepository.deleteAllByExpiresAtBefore(now);
    passwordResetTokenRepository.deleteByUser(user);

    // 새 토큰을 만들어 Database 에 저장합니다.
    String tokenValue = generateToken();
    PasswordResetToken token = PasswordResetToken.builder()
        .user(user)
        .token(tokenValue)
        .createdAt(now)
        .expiresAt(now.plus(passwordResetProperties.getTokenExpiry()))
        .build();
    passwordResetTokenRepository.save(token);

    // 안내 메일을 발송합니다.
    sendResetEmail(user, tokenValue, token.getExpiresAt());
  }

  /**
   * 주어진 토큰이 아직 만료되지 않았고, 사용 처리되지 않았는지 검사합니다.
   *
   * @param token 메일로 전달된 토큰 문자열
   * @return 사용 가능한 토큰이면 {@code true}, 그렇지 않으면 {@code false}
   */
  @Transactional(readOnly = true)
  public boolean isTokenUsable(String token) {
    if (!StringUtils.hasText(token)) {
      return false;
    }
    LocalDateTime now = LocalDateTime.now(clock);
    return passwordResetTokenRepository.findByToken(token)
        .filter(found -> !found.isExpired(now))
        .filter(found -> !found.isUsed())
        .isPresent();
  }

  /**
   * 토큰과 새 비밀번호가 모두 검증되면 실제 사용자 비밀번호를 변경합니다.
   * <p>
   * 이 때 토큰의 만료 여부와 사용 여부를 다시 확인해 재사용 공격을 막습니다.
   */
  @Transactional
  public void resetPassword(PasswordResetForm form) {
    LocalDateTime now = LocalDateTime.now(clock);
    PasswordResetToken token = passwordResetTokenRepository.findByToken(form.getToken())
        .orElseThrow(() -> new IllegalArgumentException("토큰이 유효하지 않습니다."));

    if (token.isExpired(now)) {
      throw new IllegalStateException("토큰이 만료되었습니다. 처음부터 다시 진행해주세요.");
    }

    if (token.isUsed()) {
      throw new IllegalStateException("이미 사용된 토큰입니다.");
    }

    User user = token.getUser();
    user.setPassword(passwordEncoder.encode(form.getNewPassword()));
    token.markUsed(now); // 동일 토큰이 다시 사용되지 않도록 사용 시각을 기록합니다.
    passwordResetTokenRepository.deleteAllByExpiresAtBefore(now); // 자연스럽게 만료된 토큰도 청소합니다.
  }

  /**
   * UUID 를 기반으로 하이픈이 제거된 임의 토큰 문자열을 생성합니다.
   */
  private String generateToken() {
    return UUID.randomUUID().toString().replace("-", "");
  }

  /**
   * 사용자의 이메일로 재설정 안내 메일을 발송합니다.
   */
  private void sendResetEmail(User user, String tokenValue, LocalDateTime expiresAt) {
    MimeMessage mimeMessage = mailSender.createMimeMessage();
    try {
      MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
      helper.setTo(user.getEmail());
      String fromAddress = StringUtils.hasText(mailProperties.getFrom())
          ? mailProperties.getFrom()
          : "no-reply@bookstore.local";
      helper.setFrom(fromAddress);
      helper.setSubject("[Bookstore] 비밀번호 재설정 안내");
      helper.setText(buildMailBody(user, tokenValue, expiresAt), true); // true: HTML 로 전송
      mailSender.send(mimeMessage);
    } catch (MessagingException e) {
      throw new IllegalStateException("비밀번호 재설정 메일을 보내는 데 실패했습니다.", e);
    }
  }

  /**
   * HTML 문자열을 만들어 메일 본문에 채워 넣습니다.
   * <p>
   * {@link UriComponentsBuilder} 를 활용해 토큰이 포함된 URL 을 안전하게 생성합니다.
   */
  private String buildMailBody(User user, String tokenValue, LocalDateTime expiresAt) {
    String targetName = StringUtils.hasText(user.getName()) ? user.getName() : "회원";
    String resetLink = UriComponentsBuilder.fromUriString(passwordResetProperties.getBaseUrl())
        .queryParam("token", tokenValue)
        .build(true) // true: 인코딩 결과를 유지하도록 설정
        .toUriString();
    String expiryText = expiresAt.format(MAIL_TIME_FORMATTER);

    return """
        <div style=\"font-family: 'Pretendard', 'Apple SD Gothic Neo', sans-serif;\">
          <h2 style=\"color:#1d4ed8;\">비밀번호 재설정 안내</h2>
          <p>%s 님, 안녕하세요.</p>
          <p>아래 버튼을 눌러 비밀번호 재설정 절차를 마무리해주세요.</p>
          <p style=\"margin:28px 0;\">
            <a href=\"%s\" style=\"background:#1d4ed8;color:#fff;padding:12px 20px;border-radius:8px;text-decoration:none;display:inline-block;\">비밀번호 재설정하기</a>
          </p>
          <p>본 링크는 %s 까지 유효합니다.</p>
          <p>만약 본인이 요청하지 않았다면 본 메일을 무시해 주세요.</p>
          <hr style=\"margin:32px 0;border:none;border-top:1px solid #e2e8f0;\" />
          <p style=\"font-size:12px;color:#6b7280;\">Bookstore Team</p>
        </div>
        """.formatted(targetName, resetLink, expiryText);
  }
}
