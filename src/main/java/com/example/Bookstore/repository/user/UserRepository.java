package com.example.Bookstore.repository.user;

import com.example.Bookstore.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}

