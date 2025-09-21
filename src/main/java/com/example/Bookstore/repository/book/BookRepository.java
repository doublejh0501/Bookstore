package com.example.Bookstore.repository.book;

import com.example.Bookstore.domain.book.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {

    //Isbn으로 book 조회
    Optional<Book> findByIsbn(Long isbn);

    //Isbn 중복체크
    boolean existsByIsbn(Long isbn);

    //제목(대소문자 무시) 일부 키워드로 조회 + 페이징
    Page<Book> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    //출판사명 일부 키워드로 조회 + 페이징
    Page<Book> findByPublisherContainingIgnoreCase(String publisher, Pageable pageable);

    //작가이름 일부 키워드로 조회 + 페이징
    Page<Book> findByBookAuthors_Author_NameContainingIgnoreCase(String authorName, Pageable pageable);
}
