package com.example.Bookstore.bootstrap;

import com.example.Bookstore.domain.book.Book;
import com.example.Bookstore.domain.book.Inventory;
import com.example.Bookstore.repository.book.BookRepository;
import com.example.Bookstore.repository.book.InventoryRepository;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Initializes default inventory rows for books that don't have one yet.
 * - Creates Inventory with quantity=5 for each missing book
 * - Does NOT overwrite existing inventory
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DefaultInventoryInitializer implements ApplicationRunner {

  private final BookRepository bookRepository;
  private final InventoryRepository inventoryRepository;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    List<Book> noInventory = bookRepository.findByInventoryIsNull();
    if (noInventory.isEmpty()) {
      log.info("Inventory bootstrap: nothing to create");
      return;
    }
    int created = 0;
    for (Book book : noInventory) {
      if (book == null || book.getId() == null) continue;
      Inventory inv = Inventory.builder()
          .book(book)
          .quantity(5L)
          .build();
      inventoryRepository.save(inv);
      created++;
    }
    log.info("Inventory bootstrap: created {} inventory rows with default quantity=5", created);
  }
}

