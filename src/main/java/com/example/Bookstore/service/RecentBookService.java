package com.example.Bookstore.service;

import com.example.Bookstore.domain.book.Book;
import com.example.Bookstore.domain.book.RecentBook;
import com.example.Bookstore.repository.book.BookRepository;
import com.example.Bookstore.repository.book.RecentBookRepository;
import com.example.Bookstore.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RecentBookService {

    private final RecentBookRepository recentBookRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    // 최근 본 도서 기록: 기존 항목이 있으면 삭제 후 다시 저장해서 항상 최신 순 유지
    public void recordRecentBook(Long userId, Long bookId){
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음 : " + userId));
        var book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("책 없음 : " + bookId));

        recentBookRepository.findByUser_IdAndBook_Id(userId, bookId)
                .ifPresent(recentBookRepository::delete);

        recentBookRepository.save(
                RecentBook.builder()
                        .user(user)
                        .book(book)
                        .build()
        );
    }

    //5개만 조회하기
    @Transactional(readOnly = true)
    public List<Book> getRecentBooks (Long userId) {
        return recentBookRepository.findByUser_IdOrderByIdDesc(userId)
                .stream()
                .limit(5)
                .map(RecentBook::getBook)
                .toList();
    }
}
