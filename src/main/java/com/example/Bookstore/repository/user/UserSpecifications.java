package com.example.Bookstore.repository.user;

import com.example.Bookstore.domain.user.MemberGrade;
import com.example.Bookstore.domain.user.MemberStatus;
import com.example.Bookstore.domain.user.User;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public class UserSpecifications {

    public static Specification<User> idEquals(Long id) {
        return (root, query, cb) -> id == null ? null : cb.equal(root.get("id"), id);
    }

    public static Specification<User> nameContains(String name) {
        return (root, query, cb) -> (name == null || name.isBlank()) ? null :
                cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<User> emailContains(String email) {
        return (root, query, cb) -> (email == null || email.isBlank()) ? null :
                cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%");
    }

    public static Specification<User> statusEquals(MemberStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<User> gradeEquals(MemberGrade grade) {
        return (root, query, cb) -> grade == null ? null : cb.equal(root.get("grade"), grade);
    }

    public static Specification<User> createdBetween(LocalDateTime from, LocalDateTime to) {
        return (root, query, cb) -> {
            if (from == null && to == null) return null;
            if (from != null && to != null) {
                return cb.between(root.get("createdAt"), from, to);
            } else if (from != null) {
                return cb.greaterThanOrEqualTo(root.get("createdAt"), from);
            } else {
                return cb.lessThan(root.get("createdAt"), to);
            }
        };
    }
}

