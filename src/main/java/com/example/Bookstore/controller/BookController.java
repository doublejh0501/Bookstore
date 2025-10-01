package com.example.Bookstore.controller;

import com.example.Bookstore.domain.book.Book;
import com.example.Bookstore.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


@Controller
@RequiredArgsConstructor
@RequestMapping("/books")
public class BookController {

    private final BookService bookService;

    //전체 : /books
    //특정 카테고리 : /books?categoryId=3
    //제목 검색 : /books?field=title&keyword=토지
    //저자 검색 : /books?field=author&keyword=박경리
    //카테고리 + 검색 : /books?categoryId=3&field=publisher&keyword=문학동네

    @GetMapping
    public String books(@RequestParam(required = false) Long categoryId,
                        @RequestParam(required = false) String keyword,
                        @RequestParam(defaultValue = "title") String field,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "12") int size,
                        Model model) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<Book> books;

        //카테고리 + 검색 동시
        if (categoryId != null && keyword != null && !keyword.isBlank()) {
            books = switch (field) {
                case "author" ->
                        bookService.getBooksByCategoryIdAndAuthorContainingIgnoreCase(categoryId, keyword, pageable);
                case "publisher" ->
                        bookService.getBooksByCategoryIdAndPublisherContainingIgnoreCase(categoryId, keyword, pageable);
                default -> bookService.getBooksByCategoryIdAndTitleContainingIgnoreCase(categoryId, keyword, pageable);
            };
        }

        //카테고리
        else if (categoryId != null) {
            books = bookService.getBooksByCategoryId(categoryId, pageable);
        }

        //검색
        else if (keyword != null && !keyword.isBlank()) {
            books = switch (field) {
                case "author" -> bookService.getBooksByAuthorContainingIgnoreCase(keyword, pageable);
                case "publisher" -> bookService.getBooksByPublisherContainingIgnoreCase(keyword, pageable);
                case "all" -> bookService.getBooksByAllFields(keyword, pageable);
                default -> bookService.getBooksByTitleContainingIgnoreCase(keyword, pageable);
            };
        }

        //전체
        else {
            books = bookService.getAllBooks(pageable);
        }

        model.addAttribute("books", books.getContent());
        model.addAttribute("page", books);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("keyword", keyword);
        model.addAttribute("field", field);

        return "book/books";
    }

    @GetMapping("/{id}")
    public String getBookDetail(@PathVariable Long id, Model model) {
        Book book = bookService.getBookById(id);
        model.addAttribute("book", book);
        return "book/detail";

    }
}