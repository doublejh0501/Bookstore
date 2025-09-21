package com.example.Bookstore.repository.review;

import com.example.Bookstore.domain.review.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    //특정 책 리뷰 조회
    Page<Review> findByBook_Id(Long bookId, Pageable pageable);

    //특정 사용자 작성 리뷰 조회
    List<Review> findByUser_Id(Long userId);

    //특정 사용자 특정 책에 남긴 리뷰 단건 조회
    Optional<Review> findByUser_IdAndBook_Id(Long userId, Long bookId);

    //특정 책 리뷰 개수
    long countByBook_Id(Long bookId);

}
