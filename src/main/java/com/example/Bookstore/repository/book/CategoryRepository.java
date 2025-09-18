package com.example.Bookstore.repository.book;

import com.example.Bookstore.domain.book.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    //이름으로 카테고리 조회
    Optional<Category> findByName (String name);

    //카테고리 이름 중복체크
    boolean existsByName(String name);

    //카테고리 이름 일부 검색 + 페이징
    Page<Category> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
