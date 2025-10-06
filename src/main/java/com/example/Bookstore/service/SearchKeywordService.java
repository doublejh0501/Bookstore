package com.example.Bookstore.service;


import com.example.Bookstore.domain.search.SearchKeywordStat;
import com.example.Bookstore.repository.search.SearchKeywordStatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class SearchKeywordService {

    private final SearchKeywordStatRepository searchKeywordStatRepository;

    //인기 검색어 순위
    //1. 키워드가 존재하면 +1
    public void recordSearch(String keyword) {
        String q = (keyword == null) ? "" : keyword.trim();
        if(q.isEmpty()) return;

        Optional<SearchKeywordStat> opt =  searchKeywordStatRepository.findByKeyword(q);
        if(opt.isPresent()) {
            SearchKeywordStat stat = opt.get();
            stat.setCount(stat.getCount() + 1);
            stat.setLastSearchedAt(LocalDateTime.now());
        }
        //2. 키워드 없으면 새로 생성 count = 1
        else {
            searchKeywordStatRepository.save(
                    SearchKeywordStat.builder()
                            .keyword(q)
                            .count(1L)
                            .lastSearchedAt(LocalDateTime.now())
                            .build()
            );
        }
    }

    @Transactional(readOnly = true)
    public List<SearchKeywordStat> getTopKeywords() {
        return searchKeywordStatRepository.findTop10ByOrderByCountDesc();
    }
}
