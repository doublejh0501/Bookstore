package com.example.Bookstore.service;

import com.example.Bookstore.repository.order.OrderItemRepository;
import com.example.Bookstore.repository.search.SearchKeywordStatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BookRankingService {

    private final OrderItemRepository orderItemRepository;

    //월간 베스트셀러 조회
    public List<Object[]> getMonthlyBestsellers() {
        LocalDateTime start = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime end = LocalDateTime.now();

        return orderItemRepository.findMonthlyBestsellers(start, end);
    }







}
