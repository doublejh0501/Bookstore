package com.example.Bookstore.repository.book;

import com.example.Bookstore.domain.book.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long>, JpaSpecificationExecutor<Inventory> {

    //책 재고 조회
    Optional<Inventory> findByBook_Id(Long bookId);

    //책 존재 여부 확인
    boolean existsByBook_Id(Long bookId);
}
