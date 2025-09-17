package com.example.Bookstore.repository.user;

import com.example.Bookstore.domain.user.MemberGrade;
import com.example.Bookstore.domain.user.MemberStatus;
import com.example.Bookstore.domain.user.Role;
import com.example.Bookstore.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // username으로 유저 찾기
    Optional<User> findByUsername(String username);

    // 단건 조회 (이메일/전화)
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);

    // 중복/존재 여부
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);

    // 필터링 목록 조회
    List<User> findAllByRole(Role role);
    List<User> findAllByStatus(MemberStatus status);
    List<User> findAllByGrade(MemberGrade grade);

    // 부분 문자열 검색
    List<User> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
            String firstNamePart, String lastNamePart);

    // 가입일 범위
    List<User> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    // 통합 검색(이름/이메일/아이디)
    @Query("""
           select u from User u
           where lower(u.username) like lower(concat('%', :q, '%'))
              or lower(u.email)     like lower(concat('%', :q, '%'))
              or lower(u.username)  like lower(concat('%', :q, '%'))
           """)
    List<User> search(@Param("q") String q);






}
