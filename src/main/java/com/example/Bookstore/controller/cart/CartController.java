package com.example.Bookstore.controller.cart;

import com.example.Bookstore.domain.cart.Cart;
import com.example.Bookstore.domain.cart.CartItem;
import com.example.Bookstore.domain.cart.CartStatus;
import com.example.Bookstore.repository.cart.CartItemRepository;
import com.example.Bookstore.repository.cart.CartRepository;
import com.example.Bookstore.repository.book.BookRepository;
import com.example.Bookstore.repository.user.UserRepository;
import com.example.Bookstore.security.jwt.JwtPrincipal;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/cart")
public class CartController {

  private final CartRepository cartRepository;
  private final CartItemRepository cartItemRepository;
  private final BookRepository bookRepository;
  private final UserRepository userRepository;

  public CartController(
      CartRepository cartRepository,
      CartItemRepository cartItemRepository,
      BookRepository bookRepository,
      UserRepository userRepository) {
    this.cartRepository = Objects.requireNonNull(cartRepository, "cartRepository must not be null");
    this.cartItemRepository = Objects.requireNonNull(cartItemRepository, "cartItemRepository must not be null");
    this.bookRepository = Objects.requireNonNull(bookRepository, "bookRepository must not be null");
    this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
  }

  @GetMapping
  public String viewCart(@AuthenticationPrincipal JwtPrincipal principal, Model model) {
    List<ItemView> items = List.of();
    if (principal != null) {
      Long userId = principal.userId();
      Cart cart = cartRepository.findByUser_IdAndStatus(userId, CartStatus.ACTIVE).orElse(null);
      if (cart != null && cart.getItems() != null) {
        items = cart.getItems().stream().map(this::toView).toList();
      }
    }
    // compute total on server to avoid template aggregation expressions
    java.math.BigDecimal cartTotal = items.stream()
        .map(ItemView::subtotal)
        .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
    model.addAttribute("items", items);
    model.addAttribute("cartTotal", cartTotal);
    return "user/cart";
  }

  /** Add a book to current user's active cart, creating the cart if missing. */
  @PostMapping
  @Transactional
  public String add(
      @AuthenticationPrincipal JwtPrincipal principal,
      @RequestParam("bookId") Long bookId,
      @RequestParam(value = "qty", required = false, defaultValue = "1") Integer qty,
      RedirectAttributes ra) {
    if (principal == null) return "redirect:/login";
    if (qty == null || qty <= 0) qty = 1;

    var book = bookRepository.findById(bookId)
        .orElseThrow(() -> new EntityNotFoundException("Book not found: " + bookId));

    Cart cart = cartRepository.findByUser_IdAndStatus(principal.userId(), CartStatus.ACTIVE)
        .orElseGet(() -> {
          var user = userRepository.findById(principal.userId())
              .orElseThrow(() -> new EntityNotFoundException("User not found: " + principal.userId()));
          Cart c = Cart.builder().user(user).status(CartStatus.ACTIVE).build();
          return cartRepository.save(c);
        });

    var existing = cartItemRepository.findByCart_IdAndBook_Id(cart.getId(), book.getId());
    if (existing.isPresent()) {
      CartItem item = existing.get();
      long q = item.getQuantity() == null ? 0L : item.getQuantity();
      item.setQuantity(q + qty);
      cartItemRepository.save(item);
    } else {
      CartItem item = CartItem.builder()
          .cart(cart)
          .book(book)
          .quantity(qty.longValue())
          .build();
      cartItemRepository.save(item);
    }

    ra.addFlashAttribute("message", "장바구니에 담았습니다.");
    return "redirect:/cart";
  }

  @PostMapping("/inc")
  public String inc(@AuthenticationPrincipal JwtPrincipal principal, @RequestParam("_id") Long itemId) {
    if (principal == null) return "redirect:/login";
    CartItem item = loadOwnedItem(principal.userId(), itemId);
    long q = item.getQuantity() == null ? 0L : item.getQuantity();
    item.setQuantity(q + 1);
    cartItemRepository.save(item);
    return "redirect:/cart";
  }

  @PostMapping("/dec")
  public String dec(@AuthenticationPrincipal JwtPrincipal principal, @RequestParam("_id") Long itemId) {
    if (principal == null) return "redirect:/login";
    CartItem item = loadOwnedItem(principal.userId(), itemId);
    long q = item.getQuantity() == null ? 0L : item.getQuantity();
    if (q <= 1) {
      cartItemRepository.delete(item);
    } else {
      item.setQuantity(q - 1);
      cartItemRepository.save(item);
    }
    return "redirect:/cart";
  }

  @PostMapping("/remove")
  public String remove(@AuthenticationPrincipal JwtPrincipal principal, @RequestParam("_id") Long itemId) {
    if (principal == null) return "redirect:/login";
    CartItem item = loadOwnedItem(principal.userId(), itemId);
    cartItemRepository.delete(item);
    return "redirect:/cart";
  }

  @PostMapping("/update")
  public String update() {
    // Placeholder for bulk update if needed. Current template uses per-row inc/dec/remove.
    return "redirect:/cart";
  }

  private CartItem loadOwnedItem(Long userId, Long itemId) {
    CartItem item = cartItemRepository.findById(itemId)
        .orElseThrow(() -> new EntityNotFoundException("Cart item not found: " + itemId));
    if (item.getCart() == null || item.getCart().getUser() == null || !item.getCart().getUser().getId().equals(userId)) {
      throw new IllegalArgumentException("Access denied to cart item: " + itemId);
    }
    if (item.getCart().getStatus() != CartStatus.ACTIVE) {
      throw new IllegalStateException("Cart is not active for item: " + itemId);
    }
    return item;
  }

  private ItemView toView(CartItem item) {
    Long id = item.getId();
    var book = item.getBook();
    Long productId = book != null ? book.getId() : null;
    String productName = book != null ? book.getTitle() : "알 수 없는 도서";
    BigDecimal unitPrice = (book != null && book.getPrice() != null) ? book.getPrice() : BigDecimal.ZERO;
    int quantity = item.getQuantity() == null ? 0 : item.getQuantity().intValue();
    BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
    return new ItemView(id, productId, productName, unitPrice, quantity, subtotal);
  }

  public record ItemView(
      Long id,
      Long productId,
      String productName,
      BigDecimal unitPrice,
      int quantity,
      BigDecimal subtotal) {}
}
