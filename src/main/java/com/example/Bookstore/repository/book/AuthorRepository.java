package com.example.Bookstore.repository.book;

import com.example.Bookstore.domain.book.Author;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthorRepository extends JpaRepository<Author, Long> {

    //이름으로 Author 조회
    Optional<Author> findByName(String name);

    //이름 중복 체크
    boolean existsByName(String name);

    //이름 키워드로 작가 조회
    Page<Author> findByNameContainingIgnoreCase(String name, Pageable pageable);

    //책 제목 키워드로 작가 조회 + 페이징
    //중복 작가 제거
    @EntityGraph(attributePaths = {"bookAuthors", "bookAuthors.book"})
    Page<Author> findDistinctByBookAuthors_Book_TitleContainingIgnoreCase(String title, Pageable pageable);

    //출판사 키워드로 작가 조회 + 페이징
    //중복 출판사 제거
    @EntityGraph(attributePaths = {"bookAuthors", "bookAuthors.book"})
    Page<Author> findDistinctByBookAuthors_Book_PublisherContainingIgnoreCase(String publisher, Pageable pageable);
}
