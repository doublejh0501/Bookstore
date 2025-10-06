package com.example.Bookstore.controller;

import com.example.Bookstore.domain.book.Book;
import com.example.Bookstore.service.RecentBookService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/recent-books")
public class RecentBookController {

    private final RecentBookService recentBookService;

    @GetMapping("/{userId}")
    public String showRecentBooks(@PathVariable Long userId, Model model){
        List<Book> recentBooks = recentBookService.getRecentBooks(userId);
        model.addAttribute("recentBooks", recentBooks);
        return "book/home";
    }
}
