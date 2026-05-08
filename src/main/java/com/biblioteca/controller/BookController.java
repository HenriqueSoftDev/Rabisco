package com.biblioteca.controller;

import com.biblioteca.dto.BookRequest;
import com.biblioteca.dto.BookResponse;
import com.biblioteca.model.BookStatus;
import com.biblioteca.repository.UserRepository;
import com.biblioteca.service.BookService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;
    private final UserRepository userRepository;

    public BookController(BookService bookService, UserRepository userRepository) {
        this.bookService = bookService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<BookResponse> create(@Valid @RequestBody BookRequest request,
                                               @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookService.create(request, getUserId(principal)));
    }

    @GetMapping
    public ResponseEntity<List<BookResponse>> list(@AuthenticationPrincipal UserDetails principal,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(required = false) String q) {
        String userId = getUserId(principal);
        List<BookResponse> books;
        if (q != null && !q.isBlank()) {
            books = bookService.search(userId, q);
        } else if (status != null && !status.isBlank()) {
            books = bookService.findByStatus(userId, BookStatus.valueOf(status.toUpperCase()));
        } else {
            books = bookService.findAll(userId);
        }
        return ResponseEntity.ok(books);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookResponse> get(@PathVariable String id,
                                            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(bookService.findById(id, getUserId(principal)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BookResponse> update(@PathVariable String id,
                                               @Valid @RequestBody BookRequest request,
                                               @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(bookService.update(id, request, getUserId(principal)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id,
                                       @AuthenticationPrincipal UserDetails principal) {
        bookService.delete(id, getUserId(principal));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> stats(@AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(bookService.getStats(getUserId(principal)));
    }

    private String getUserId(UserDetails principal) {
        return userRepository.findByUsername(principal.getUsername())
                .map(u -> u.getId())
                .orElseThrow();
    }
}
