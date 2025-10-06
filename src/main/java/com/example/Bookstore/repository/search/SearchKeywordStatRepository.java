package com.example.Bookstore.repository.search;

import com.example.Bookstore.domain.search.SearchKeywordStat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SearchKeywordStatRepository extends JpaRepository<SearchKeywordStat, Long> {

    Optional<SearchKeywordStat> findByKeyword(String keyword);

    List<SearchKeywordStat> findTop10ByOrderByCountDesc();
}
