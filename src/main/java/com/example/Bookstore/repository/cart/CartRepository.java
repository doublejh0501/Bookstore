package com.example.Bookstore.repository.cart;

import com.example.Bookstore.domain.cart.Cart;
import com.example.Bookstore.domain.cart.CartStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    //사용자 장바구니 조회
    Optional<Cart> findByUser_Id(Long userId);

    //사용자 + 상태 조합 장바구니 조회
    Optional<Cart> findByUser_IdAndStatus(Long userId, CartStatus status);

    //장바구니 중복체크
    boolean existsByUser_Id(Long userId);

    //상태별 장바구니 조회
    List<Cart> findByStatus(CartStatus status);

}
