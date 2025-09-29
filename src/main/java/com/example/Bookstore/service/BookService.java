package com.example.Bookstore.service;

import com.example.Bookstore.domain.book.Book;
import com.example.Bookstore.repository.book.*;
import com.example.Bookstore.repository.order.OrderItemRepository;
import com.example.Bookstore.repository.search.SearchKeywordStatRepository;
import com.example.Bookstore.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookService {

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;

    //메뉴 분류별 조회
    public Page<Book> getBooksByCategoryId(Long categoryId, Pageable pageable) {
        if(!categoryRepository.existsById(categoryId)){
            throw new IllegalArgumentException("존재하지 않는 카테고리입니다. id = " + categoryId);
        }
        return bookRepository.findByCategory_Id(categoryId, pageable);
    }

    //책 목록 전체
    public Page<Book> getAllBooks(Pageable pageable) {
        return bookRepository.findAll(pageable);
    }

    //검색 기능
    //1. 책 제목으로 조회
    public Page<Book> getBooksByTitleContainingIgnoreCase(String title, Pageable pageable) {
        String q = title == null ? "" : title.trim();
        if(q.isEmpty()) return Page.empty(pageable);
        return bookRepository.findByTitleContainingIgnoreCase(q, pageable);
    }

    //2. 출판사이름으로 조회
    public Page<Book> getBooksByPublisherContainingIgnoreCase(String publisher, Pageable pageable) {
        String q = publisher == null ? "" : publisher.trim();
        if(q.isEmpty()) return Page.empty(pageable);
        return bookRepository.findByPublisherContainingIgnoreCase(q, pageable);
    }

    //3. 저자이름으로 조회
    public Page<Book> getBooksByAuthorContainingIgnoreCase(String authorName, Pageable pageable) {
        String q = authorName == null ? "" : authorName.trim();
        if(q.isEmpty()) return Page.empty(pageable);
        return bookRepository.findByBookAuthors_Author_NameContainingIgnoreCase(q, pageable);
    }

    //4. 카테고리 + 책 제목 조회
    public Page<Book> getBooksByCategoryIdAndTitleContainingIgnoreCase(Long categoryId, String title, Pageable pageable) {
        String q = title == null ? "" : title.trim();
        if( categoryId == null || q.isEmpty() ) return Page.empty(pageable);
        return bookRepository.findByCategory_IdAndTitleContainingIgnoreCase(categoryId, q, pageable);
    }

    //5. 카테고리 + 출판사
    public Page<Book> getBooksByCategoryIdAndPublisherContainingIgnoreCase(Long categoryId, String publisher, Pageable pageable) {
        String q = publisher == null ? "" : publisher.trim();
        if( categoryId == null || q.isEmpty() ) return Page.empty(pageable);
        return bookRepository.findByCategory_IdAndPublisherContainingIgnoreCase(categoryId, q, pageable);
    }

    //6. 카테고리 + 저자
    public Page<Book> getBooksByCategoryIdAndAuthorContainingIgnoreCase(Long categoryId, String authorName, Pageable pageable) {
        String q = authorName == null ? "" : authorName.trim();
        if( categoryId == null || q.isEmpty() ) return Page.empty(pageable);
        return bookRepository.findByCategory_IdAndBookAuthors_Author_NameContainingIgnoreCase(categoryId, q, pageable);
    }

    public Page<Book> getBooksByAllFields(String keyword, Pageable pageable) {
        return bookRepository.findByTitleContainingIgnoreCaseOrBookAuthors_Author_NameContainingIgnoreCaseOrPublisherContainingIgnoreCase(
                keyword, keyword, keyword, pageable
        );
    }

    public Book getBookById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 책을 찾을 수 없습니다. id = " + id));
    }
}
