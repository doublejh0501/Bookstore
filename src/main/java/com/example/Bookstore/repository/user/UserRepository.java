package com.example.Bookstore.repository.user;

import com.example.Bookstore.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // username으로 유저 찾기
    Optional<User> findByUsername(String username);






}

