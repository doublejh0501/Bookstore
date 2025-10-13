package com.example.Bookstore.controller.admin;

import com.example.Bookstore.domain.book.Book;
import com.example.Bookstore.domain.book.Inventory;
import com.example.Bookstore.domain.book.SaleStatus;
import com.example.Bookstore.repository.book.BookRepository;
import com.example.Bookstore.repository.book.InventoryRepository;
import com.example.Bookstore.repository.book.BookSpecifications;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.ArrayList;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/products")
public class AdminProductController {

  private final BookRepository bookRepository;
  private final InventoryRepository inventoryRepository;

  public AdminProductController(BookRepository bookRepository, InventoryRepository inventoryRepository) {
    this.bookRepository = Objects.requireNonNull(bookRepository);
    this.inventoryRepository = Objects.requireNonNull(inventoryRepository);
  }

  @GetMapping
  public String list(
      @RequestParam(value = "q", required = false) String q,
      @RequestParam(value = "saleStatus", required = false) String saleStatus,
      @RequestParam(value = "from", required = false) String from,
      @RequestParam(value = "to", required = false) String to,
      @RequestParam(value = "stockMin", required = false) String stockMin,
      @RequestParam(value = "stockMax", required = false) String stockMax,
      @PageableDefault(size = 30) Pageable pageable,
      Model model) {

    Page<Book> page;
    Map<Long, Long> stockMap;

    java.time.LocalDate fromDate = null, toDate = null;
    try { if (from != null && !from.isBlank()) fromDate = java.time.LocalDate.parse(from); } catch (Exception ignore) {}
    try { if (to != null && !to.isBlank()) toDate = java.time.LocalDate.parse(to); } catch (Exception ignore) {}
    com.example.Bookstore.domain.book.SaleStatus stEnum = null;
    if (saleStatus != null && !saleStatus.isBlank()) {
      try { stEnum = com.example.Bookstore.domain.book.SaleStatus.valueOf(saleStatus); } catch (Exception ignore) {}
    }
    Long minStock = null, maxStock = null;
    try { if (stockMin != null && !stockMin.isBlank()) minStock = Long.valueOf(stockMin); } catch (Exception ignore) {}
    try { if (stockMax != null && !stockMax.isBlank()) maxStock = Long.valueOf(stockMax); } catch (Exception ignore) {}

    var spec = org.springframework.data.jpa.domain.Specification.where(BookSpecifications.titleOrPublisherContains(q))
        .or(BookSpecifications.authorNameContains(q))
        .and(BookSpecifications.saleStatusEquals(stEnum))
        .and(BookSpecifications.createdBetween(
            fromDate != null ? fromDate.atStartOfDay() : null,
            toDate != null ? toDate.plusDays(1).atStartOfDay() : null
        ))
        .and(BookSpecifications.stockBetween(minStock, maxStock));

    page = bookRepository.findAll(spec, pageable);

    var bookIds = page.getContent().stream().map(Book::getId).toList();
    stockMap = inventoryRepository.findByBook_IdIn(bookIds).stream()
        .filter(inv -> inv.getBook() != null && inv.getBook().getId() != null)
        .collect(Collectors.toMap(inv -> inv.getBook().getId(), inv -> inv.getQuantity() == null ? 0L : inv.getQuantity(), (a,b) -> a));

    List<ProductListItem> items = new ArrayList<>();
    for (Book b : page.getContent()) {
      long qty = stockMap.getOrDefault(b.getId(), 0L);
      items.add(toListItem(b, qty));
    }

    model.addAttribute("items", items);
    model.addAttribute("page", page.getNumber());
    model.addAttribute("pageSize", page.getSize());
    model.addAttribute("totalPages", page.getTotalPages());
    model.addAttribute("q", q);
    model.addAttribute("saleStatus", saleStatus);
    model.addAttribute("from", from);
    model.addAttribute("to", to);
    model.addAttribute("stockMin", stockMin);
    model.addAttribute("stockMax", stockMax);
    return "admin/product-list";
  }

  @GetMapping("/{id}")
  public String detail(@PathVariable("id") Long id, Model model) {
    Book b = bookRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Book not found: " + id));
    long stock = inventoryRepository.findByBook_Id(id).map(Inventory::getQuantity).orElse(0L);
    ProductDetail product = toDetail(b, stock);
    model.addAttribute("product", product);
    return "admin/product-detail";
  }

  @GetMapping("/new")
  public String newForm(Model model) {
    model.addAttribute("productForm", new ProductForm(null, null, null, null, null, null, null, null, null, null));
    model.addAttribute("formAction", "#");
    return "admin/product-form";
  }

  @GetMapping("/{id}/edit")
  public String editForm(@PathVariable("id") Long id, Model model) {
    Book b = bookRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Book not found: " + id));
    String author = b.getBookAuthors() == null ? null : b.getBookAuthors().stream()
        .map(ba -> ba.getAuthor() != null ? ba.getAuthor().getName() : null)
        .filter(Objects::nonNull)
        .collect(Collectors.joining(", "));
    ProductForm form = new ProductForm(
        b.getIsbn(),
        b.getTitle(),
        b.getPublisher(),
        author,
        b.getPrice() != null ? b.getPrice().longValue() : null,
        b.getSize(),
        b.getRating() != null ? b.getRating().doubleValue() : null,
        b.getViewCnt() != null ? b.getViewCnt().longValue() : null,
        b.getSaleStatus() != null ? b.getSaleStatus().name() : null,
        b.getDescription()
    );
    model.addAttribute("productForm", form);
    model.addAttribute("formAction", "#");
    return "admin/product-form";
  }

  private ProductListItem toListItem(Book b, Long stock) {
    String author = b.getBookAuthors() == null ? null : b.getBookAuthors().stream()
        .map(ba -> ba.getAuthor() != null ? ba.getAuthor().getName() : null)
        .filter(Objects::nonNull)
        .collect(Collectors.joining(", "));
    return new ProductListItem(
        b.getId(),
        b.getImageUrl(),
        b.getTitle(),
        author,
        b.getPublisher(),
        b.getPrice() != null ? b.getPrice() : BigDecimal.ZERO,
        stock != null ? stock : 0,
        b.getSaleStatus() != null ? b.getSaleStatus() : SaleStatus.ON_SALE,
        b.getCreatedAt()
    );
  }

  private ProductDetail toDetail(Book b, long stock) {
    String author = b.getBookAuthors() == null ? null : b.getBookAuthors().stream()
        .map(ba -> ba.getAuthor() != null ? ba.getAuthor().getName() : null)
        .filter(Objects::nonNull)
        .collect(Collectors.joining(", "));
    return new ProductDetail(
        b.getId(),
        b.getIsbn(),
        b.getTitle(),
        b.getPublisher(),
        author,
        b.getPrice() != null ? b.getPrice() : BigDecimal.ZERO,
        stock,
        b.getSaleStatus() != null ? b.getSaleStatus() : SaleStatus.ON_SALE,
        b.getSize(),
        b.getRating(),
        b.getViewCnt(),
        b.getImageUrl(),
        b.getDescription()
    );
  }

  public record ProductListItem(
      Long id,
      String thumbnail,
      String name,
      String author,
      String publisher,
      BigDecimal price,
      long stock,
      SaleStatus saleStatus,
      LocalDateTime createdAt) {}

  public record ProductDetail(
      Long id,
      Long isbn,
      String name,
      String publisher,
      String author,
      BigDecimal price,
      long stock,
      SaleStatus saleStatus,
      String size,
      BigDecimal rating,
      Integer saleIndex,
      String thumbnail,
      String description) {}

  public record ProductForm(
      Long isbn,
      String name,
      String publisher,
      String author,
      Long price,
      String size,
      Double rating,
      Long saleIndex,
      String saleStatus,
      String description
  ) {}
}
