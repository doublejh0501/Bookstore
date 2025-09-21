package com.example.Bookstore.repository.book;

import com.example.Bookstore.domain.book.BookAuthor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookAuthorRepository extends JpaRepository<BookAuthor, Long> {

    //특정 book에 연결된 author 찾기
    List<BookAuthor> findByBook_Id(Long bookId);

    //특정 author에 연결된 book 찾기
    List<BookAuthor> findByAuthor_Id(Long authorId);

    //book + author 중복체크
    boolean existsByBook_IdAndAuthor_Id(Long bookId, Long authorId);

    //book + author 단건 조회
    Optional<BookAuthor> findByBook_IdAndAuthor_Id(Long bookId, Long authorId);

    //특정 author 이름으로 연결된 관계 조회
    List<BookAuthor> findByAuthor_NameContainingIgnoreCase(String name);
}
