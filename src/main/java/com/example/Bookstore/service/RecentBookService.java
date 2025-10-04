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

    //RecentBook 중복 체크
    public void recordRecentBook(Long userId, Long bookId){
        if(!recentBookRepository.existsByUser_IdAndBook_Id(userId, bookId)){
            var user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자 없음 : " + userId));
            var book = bookRepository.findById(bookId)
                    .orElseThrow(() -> new IllegalArgumentException("책 없음 : " + bookId));

            recentBookRepository.save(
                    RecentBook.builder()
                            .user(user)
                            .book(book)
                            .build()
            );
        }
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
