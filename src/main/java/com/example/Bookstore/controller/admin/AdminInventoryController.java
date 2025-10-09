package com.example.Bookstore.controller.admin;

import com.example.Bookstore.domain.book.Inventory;
import com.example.Bookstore.repository.book.InventoryRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/inventory")
public class AdminInventoryController {

  private final InventoryRepository inventoryRepository;

  public AdminInventoryController(InventoryRepository inventoryRepository) {
    this.inventoryRepository = Objects.requireNonNull(inventoryRepository);
  }

  @GetMapping
  public String list(
      @org.springframework.web.bind.annotation.RequestParam(value = "q", required = false) String q,
      @org.springframework.web.bind.annotation.RequestParam(value = "from", required = false) String from,
      @org.springframework.web.bind.annotation.RequestParam(value = "to", required = false) String to,
      @PageableDefault(size = 30) Pageable pageable, Model model) {

    // 단순 접근: 정렬만 DB에 위임하고, 검색/기간 필터는 메모리에서 처리합니다.
    List<Inventory> allSorted = inventoryRepository.findAll(pageable.getSort());
    java.util.stream.Stream<Inventory> stream = allSorted.stream();

    if (q != null && !q.isBlank()) {
      final String needle = q.toLowerCase();
      stream = stream.filter(inv -> {
        if (inv.getBook() == null) return false;
        var b = inv.getBook();
        boolean match = false;
        if (b.getTitle() != null && b.getTitle().toLowerCase().contains(needle)) match = true;
        if (!match && b.getPublisher() != null && b.getPublisher().toLowerCase().contains(needle)) match = true;
        if (!match && b.getBookAuthors() != null) {
          match = b.getBookAuthors().stream().anyMatch(ba -> ba.getAuthor() != null &&
              ba.getAuthor().getName() != null && ba.getAuthor().getName().toLowerCase().contains(needle));
        }
        return match;
      });
    }

    java.time.LocalDate fromDate = null;
    java.time.LocalDate toDate = null;
    try { if (from != null && !from.isBlank()) fromDate = java.time.LocalDate.parse(from); } catch (Exception ignore) {}
    try { if (to != null && !to.isBlank()) toDate = java.time.LocalDate.parse(to); } catch (Exception ignore) {}
    if (fromDate != null) {
      final java.time.LocalDate fd = fromDate;
      stream = stream.filter(inv -> inv.getBook() != null && inv.getBook().getCreatedAt() != null &&
          !inv.getBook().getCreatedAt().isBefore(fd.atStartOfDay()));
    }
    if (toDate != null) {
      final java.time.LocalDate td = toDate;
      stream = stream.filter(inv -> inv.getBook() != null && inv.getBook().getCreatedAt() != null &&
          inv.getBook().getCreatedAt().isBefore(td.plusDays(1).atStartOfDay()));
    }

    List<Inventory> filtered = stream.collect(Collectors.toList());

    int start = (int) pageable.getOffset();
    int end = Math.min(start + pageable.getPageSize(), filtered.size());
    List<Inventory> pageContent = start > filtered.size() ? List.of() : filtered.subList(start, end);
    Page<Inventory> page = new PageImpl<>(pageContent, pageable, filtered.size());

    List<Row> items = page.getContent().stream().map(inv -> new Row(
        inv.getBook() != null ? inv.getBook().getTitle() : null,
        inv.getBook() != null ? inv.getBook().getIsbn() : null,
        inv.getQuantity() != null ? inv.getQuantity() : 0L,
        0L,
        inv.getBook() != null && inv.getBook().getSaleStatus() != null ? inv.getBook().getSaleStatus().name() : null,
        inv.getBook() != null ? inv.getBook().getPrice() : java.math.BigDecimal.ZERO,
        inv.getBook() != null ? inv.getBook().getCreatedAt() : null
    )).toList();
    model.addAttribute("items", items);
    model.addAttribute("page", page.getNumber());
    model.addAttribute("pageSize", page.getSize());
    model.addAttribute("totalPages", page.getTotalPages());
    model.addAttribute("q", q);
    model.addAttribute("from", from);
    model.addAttribute("to", to);
    return "admin/inventory-list";
  }

  public record Row(String productName, Long isbn, Long quantity, Long safetyStock,
                    String saleStatus, java.math.BigDecimal price, LocalDateTime lastMovementAt) {}
}
