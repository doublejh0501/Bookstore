package com.example.Bookstore.controller;

import com.example.Bookstore.service.SearchKeywordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class SearchKeywordController {

    private final SearchKeywordService searchKeywordService;

    @GetMapping("/search/track")
    public String searchBooks(@RequestParam String field,
                              @RequestParam String keyword,
                              RedirectAttributes attrs) {
        searchKeywordService.recordSearch(keyword);
        attrs.addAttribute("field", field);
        attrs.addAttribute("keyword", keyword);
        return "redirect:/books";
    }
}
