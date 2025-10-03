package com.example.Bookstore.controller;

import com.example.Bookstore.security.jwt.JwtPrincipal;
import com.example.Bookstore.service.BookRankingService;
import com.example.Bookstore.service.RecentBookService;
import com.example.Bookstore.service.SearchKeywordService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final BookRankingService bookRankingService;
    private final RecentBookService recentBookService;
    private final SearchKeywordService searchKeywordService;

    @GetMapping("/")
    public String home(@AuthenticationPrincipal JwtPrincipal principal, Model model) {
        model.addAttribute("bestsellers", bookRankingService.getMonthlyBestsellers());
        model.addAttribute("topKeywords", searchKeywordService.getTopKeywords());

        if (principal != null) {
            model.addAttribute("recentBooks", recentBookService.getRecentBooks(principal.userId()));
        }

        return "book/home";
    }
}
