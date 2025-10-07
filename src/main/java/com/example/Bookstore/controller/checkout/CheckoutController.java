package com.example.Bookstore.controller.checkout;

import com.example.Bookstore.domain.cart.Cart;
import com.example.Bookstore.domain.cart.CartItem;
import com.example.Bookstore.domain.cart.CartStatus;
import com.example.Bookstore.repository.cart.CartRepository;
import com.example.Bookstore.repository.cart.CartItemRepository;
import com.example.Bookstore.repository.book.BookRepository;
import com.example.Bookstore.repository.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import com.example.Bookstore.security.jwt.JwtPrincipal;
import java.math.BigDecimal;
import java.util.Objects;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Controller
@RequestMapping
public class CheckoutController {

  private final CartRepository cartRepository;
  private final CartItemRepository cartItemRepository;
  private final BookRepository bookRepository;
  private final UserRepository userRepository;

  public CheckoutController(
      CartRepository cartRepository,
      CartItemRepository cartItemRepository,
      BookRepository bookRepository,
      UserRepository userRepository) {
    this.cartRepository = Objects.requireNonNull(cartRepository, "cartRepository must not be null");
    this.cartItemRepository = Objects.requireNonNull(cartItemRepository, "cartItemRepository must not be null");
    this.bookRepository = Objects.requireNonNull(bookRepository, "bookRepository must not be null");
    this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
  }

  /**
   * Checkout page: shows delivery form, payment method selection and order summary.
   */
  @GetMapping("/checkout")
  public String checkout(@AuthenticationPrincipal JwtPrincipal principal, Model model) {
    Long userId = principal.userId();

    Cart cart = cartRepository.findByUser_IdAndStatus(userId, CartStatus.ACTIVE)
        .orElse(null);

    BigDecimal total = BigDecimal.ZERO;
    if (cart != null && cart.getItems() != null) {
      for (CartItem ci : cart.getItems()) {
        if (ci.getBook() != null && ci.getQuantity() != null && ci.getQuantity() > 0) {
          BigDecimal unit = ci.getBook().getPrice();
          if (unit != null) {
            total = total.add(unit.multiply(BigDecimal.valueOf(ci.getQuantity())));
          }
        }
      }
    }

    model.addAttribute("userId", userId);
    model.addAttribute("totalPrice", total);
    // Simple defaults for receiver/address/phone could be read from user profile in future
    return "user/checkout";
  }

  /** Optional: add one item then go to checkout (from "바로 구매"). */
  @org.springframework.web.bind.annotation.PostMapping("/checkout")
  @org.springframework.transaction.annotation.Transactional
  public String buyNow(
      @AuthenticationPrincipal JwtPrincipal principal,
      @org.springframework.web.bind.annotation.RequestParam(value = "bookId", required = false) Long bookId,
      @org.springframework.web.bind.annotation.RequestParam(value = "qty", required = false, defaultValue = "1") Integer qty
  ) {
    if (principal == null) return "redirect:/login";
    if (bookId != null) {
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
    }
    return "redirect:/checkout";
  }
}
