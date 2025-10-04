package com.example.Bookstore.controller;

import com.example.Bookstore.service.BookRankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/ranking")
public class BookRankingController {

    private final BookRankingService bookRankingService;

    //월간 베스트셀러 페이지
    @GetMapping("/mothly-bestsellers")
    public String showMonthlyBestsellers(Model model) {
        //서비스에서 DB 결과 받아오기
        List<Object[]> results = bookRankingService.getMonthlyBestsellers();
        //모델에 담기
        model.addAttribute("bestsellers", results);
        //view 이동
        return "book/home";
    }
}
