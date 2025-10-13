package com.example.Bookstore.controller.admin;

import com.example.Bookstore.domain.user.User;
import com.example.Bookstore.repository.user.UserRepository;
import com.example.Bookstore.repository.user.UserSpecifications;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/members")
public class AdminMemberController {

  private final UserRepository userRepository;

  public AdminMemberController(UserRepository userRepository) {
    this.userRepository = Objects.requireNonNull(userRepository);
  }

  @GetMapping
  public String list(
      @RequestParam(value = "id", required = false) String id,
      @RequestParam(value = "name", required = false) String name,
      @RequestParam(value = "email", required = false) String email,
      @RequestParam(value = "status", required = false) String status,
      @RequestParam(value = "grade", required = false) String grade,
      @RequestParam(value = "from", required = false) String from,
      @RequestParam(value = "to", required = false) String to,
      @PageableDefault(size = 30) Pageable pageable,
      Model model) {

    Page<User> page;
    Long idVal = null;
    try { if (id != null && !id.isBlank()) idVal = Long.valueOf(id); } catch (Exception ignore) {}
    java.time.LocalDate fromDate = null, toDate = null;
    try { if (from != null && !from.isBlank()) fromDate = java.time.LocalDate.parse(from); } catch (Exception ignore) {}
    try { if (to != null && !to.isBlank()) toDate = java.time.LocalDate.parse(to); } catch (Exception ignore) {}
    com.example.Bookstore.domain.user.MemberStatus stEnum = null;
    if (status != null && !status.isBlank()) {
      try { stEnum = com.example.Bookstore.domain.user.MemberStatus.valueOf(status); } catch (Exception ignore) {}
    }
    com.example.Bookstore.domain.user.MemberGrade gEnum = null;
    if (grade != null && !grade.isBlank()) {
      try { gEnum = com.example.Bookstore.domain.user.MemberGrade.valueOf(grade); } catch (Exception ignore) {}
    }

    var spec = org.springframework.data.jpa.domain.Specification.where(UserSpecifications.idEquals(idVal))
        .and(UserSpecifications.nameContains(name))
        .and(UserSpecifications.emailContains(email))
        .and(UserSpecifications.statusEquals(stEnum))
        .and(UserSpecifications.gradeEquals(gEnum))
        .and(UserSpecifications.createdBetween(
            fromDate != null ? fromDate.atStartOfDay() : null,
            toDate != null ? toDate.plusDays(1).atStartOfDay() : null
        ));

    page = userRepository.findAll(spec, pageable);
    List<MemberListItem> items = page.stream().map(this::toListItem).toList();
    model.addAttribute("items", items);
    model.addAttribute("page", page.getNumber());
    model.addAttribute("pageSize", page.getSize());
    model.addAttribute("totalPages", page.getTotalPages());

    model.addAttribute("id", id);
    model.addAttribute("name", name);
    model.addAttribute("email", email);
    model.addAttribute("status", status);
    model.addAttribute("grade", grade);
    model.addAttribute("from", from);
    model.addAttribute("to", to);
    return "admin/member-list";
  }

  @GetMapping("/{id}")
  public String detail(@PathVariable("id") Long id, Model model) {
    User u = userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
    MemberDetail dto = new MemberDetail(
        u.getId(),
        u.getName(),
        u.getEmail(),
        u.getMobile(),
        u.getAddress(),
        u.getCreatedAt(),
        u.getLastLoginAt(),
        u.getGrade(),
        u.getStatus()
    );
    model.addAttribute("member", dto);
    return "admin/member-detail";
  }

  private MemberListItem toListItem(User u) {
    return new MemberListItem(
        u.getId(),
        u.getName(),
        u.getEmail(),
        u.getMobile(),
        u.getGrade() != null ? u.getGrade().name() : null,
        u.getStatus() != null ? u.getStatus().name() : null,
        u.getCreatedAt()
    );
  }

  public record MemberListItem(Long id, String name, String email, String phone, String grade,
                               String status, LocalDateTime joinedAt) {}

  public record MemberDetail(Long id, String name, String email, String phone, String address,
                             LocalDateTime joinedAt, LocalDateTime lastLoginAt,
                             com.example.Bookstore.domain.user.MemberGrade grade,
                             com.example.Bookstore.domain.user.MemberStatus status) {}
}
