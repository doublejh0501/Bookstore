package com.example.Bookstore.repository.book;

import com.example.Bookstore.domain.book.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
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

    //카테고리 속한 책 목록 반환
    Page<Book> findByCategory_Id(Long categoryId, Pageable pageable);

    //카테고리 + 책제목
    Page<Book> findByCategory_IdAndTitleContainingIgnoreCase(Long categoryId, String title, Pageable pageable);

    //카테고리 + 출판사
    Page<Book> findByCategory_IdAndPublisherContainingIgnoreCase(Long categoryId, String publisher, Pageable pageable);

    //카테고리 + 저자
    Page<Book> findByCategory_IdAndBookAuthors_Author_NameContainingIgnoreCase(Long categoryId, String author, Pageable pageable);

    //책제목 + 저자 + 출판사
    Page<Book> findByTitleContainingIgnoreCaseOrBookAuthors_Author_NameContainingIgnoreCaseOrPublisherContainingIgnoreCase(
            String title, String author, String publisher, Pageable pageable
    );
}
