package com.example.Bookstore.repository.cart;

import com.example.Bookstore.domain.cart.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    //장바구니에 담긴 책 조회
    List<CartItem> findByCart_Id(Long CartId);

    //장바구니 + 특정 책 조회
    Optional<CartItem> findByCart_IdAndBook_Id(Long cartId, Long bookId);

    //장바구니 + 특정 책 중복체크
    boolean existsByCart_IdAndBook_Id(Long cartId, Long bookId);

    //책이 어떤 장바구니에 담겼는지 조회
    List<CartItem> findByBook_Id(Long bookId);

    //장바구니 항목 개수
    long countByCart_Id(Long cartId);

}
