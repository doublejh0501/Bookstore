package com.example.Bookstore.repository.book;

import com.example.Bookstore.domain.book.RecentBook;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecentBookRepository extends JpaRepository<RecentBook, Long> {

    //사용자 최근 본 책 조회
    List<RecentBook> findByUser_Id(Long userId);

    //사용자 + 책 조합 단건 조회
    Optional<RecentBook> findByUser_IdAndBook_Id(Long userId, Long bookId);

    //사용자 + 책 조합 중복 체크
    boolean existsByUser_IdAndBook_Id(Long userId, Long bookId);

    //특정 책 본 사용자 조회
    List<RecentBook> findByBook_Id(Long bookId);

    //사용자별 최근 본 책( id 내림차순으로 최근 저장된 것부터)
    List<RecentBook> findByUser_IdOrderByIdDesc(Long userId);

}
