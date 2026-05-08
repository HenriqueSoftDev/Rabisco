package com.biblioteca.service;

import com.biblioteca.dto.BookRequest;
import com.biblioteca.dto.BookResponse;
import com.biblioteca.exception.BusinessException;
import com.biblioteca.exception.ResourceNotFoundException;
import com.biblioteca.model.Book;
import com.biblioteca.model.BookStatus;
import com.biblioteca.repository.BookRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public BookResponse create(BookRequest request, String userId) {
        if (request.getIsbn() != null && !request.getIsbn().isBlank()
                && bookRepository.existsByIsbnAndUserId(request.getIsbn(), userId)) {
            throw new BusinessException("Livro com este ISBN já existe na sua biblioteca");
        }
        Book book = mapToBook(request);
        book.setUserId(userId);
        return BookResponse.from(bookRepository.save(book));
    }

    public List<BookResponse> findAll(String userId) {
        return bookRepository.findByUserId(userId)
                .stream().map(BookResponse::from).toList();
    }

    public List<BookResponse> findByStatus(String userId, BookStatus status) {
        return bookRepository.findByUserIdAndStatus(userId, status)
                .stream().map(BookResponse::from).toList();
    }

    public List<BookResponse> search(String userId, String query) {
        return bookRepository.searchByUserIdAndQuery(userId, query)
                .stream().map(BookResponse::from).toList();
    }

    public BookResponse findById(String id, String userId) {
        return BookResponse.from(getBookOwnedBy(id, userId));
    }

    public BookResponse update(String id, BookRequest request, String userId) {
        Book book = getBookOwnedBy(id, userId);
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setIsbn(request.getIsbn());
        book.setGenre(request.getGenre());
        book.setPublishedYear(request.getPublishedYear());
        book.setSynopsis(request.getSynopsis());
        book.setCoverUrl(request.getCoverUrl());
        book.setStatus(request.getStatus());
        book.setRating(request.getRating());
        book.setUpdatedAt(LocalDateTime.now());
        return BookResponse.from(bookRepository.save(book));
    }

    public void delete(String id, String userId) {
        Book book = getBookOwnedBy(id, userId);
        bookRepository.delete(book);
    }

    public Map<String, Long> getStats(String userId) {
        long total = bookRepository.countByUserId(userId);
        long reading = bookRepository.countByUserIdAndStatus(userId, BookStatus.READING);
        long read = bookRepository.countByUserIdAndStatus(userId, BookStatus.READ);
        long wantToRead = bookRepository.countByUserIdAndStatus(userId, BookStatus.WANT_TO_READ);
        return Map.of("total", total, "reading", reading, "read", read, "wantToRead", wantToRead);
    }

    private Book getBookOwnedBy(String id, String userId) {
        return bookRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Livro não encontrado: " + id));
    }

    private Book mapToBook(BookRequest req) {
        Book book = new Book();
        book.setTitle(req.getTitle());
        book.setAuthor(req.getAuthor());
        book.setIsbn(req.getIsbn());
        book.setGenre(req.getGenre());
        book.setPublishedYear(req.getPublishedYear());
        book.setSynopsis(req.getSynopsis());
        book.setCoverUrl(req.getCoverUrl());
        book.setStatus(req.getStatus() != null ? req.getStatus() : BookStatus.WANT_TO_READ);
        book.setRating(req.getRating());
        return book;
    }
}
