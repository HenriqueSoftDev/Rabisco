package com.biblioteca.controller;

import com.biblioteca.model.BookStatus;
import com.biblioteca.repository.UserRepository;
import com.biblioteca.service.BookService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class WebController {

    private final BookService bookService;
    private final UserRepository userRepository;

    public WebController(BookService bookService, UserRepository userRepository) {
        this.bookService = bookService;
        this.userRepository = userRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails principal,
                            @RequestParam(required = false) String status,
                            @RequestParam(required = false) String q,
                            Model model) {
        String userId = getUserId(principal);
        var stats = bookService.getStats(userId);

        if (q != null && !q.isBlank()) {
            model.addAttribute("books", bookService.search(userId, q));
            model.addAttribute("search", q);
        } else if (status != null && !status.isBlank()) {
            BookStatus bookStatus = BookStatus.valueOf(status.toUpperCase());
            model.addAttribute("books", bookService.findByStatus(userId, bookStatus));
            model.addAttribute("activeStatus", status.toUpperCase());
        } else {
            model.addAttribute("books", bookService.findAll(userId));
        }

        model.addAttribute("stats", stats);
        model.addAttribute("statuses", BookStatus.values());
        return "dashboard";
    }

    @GetMapping("/books/new")
    public String newBookPage(Model model) {
        model.addAttribute("statuses", BookStatus.values());
        model.addAttribute("book", new com.biblioteca.dto.BookRequest());
        model.addAttribute("mode", "new");
        return "books/form";
    }

    @GetMapping("/books/{id}/edit")
    public String editBookPage(@PathVariable String id,
                               @AuthenticationPrincipal UserDetails principal,
                               Model model) {
        String userId = getUserId(principal);
        var book = bookService.findById(id, userId);
        model.addAttribute("book", book);
        model.addAttribute("statuses", BookStatus.values());
        model.addAttribute("mode", "edit");
        return "books/form";
    }

    @GetMapping("/books/{id}")
    public String bookDetail(@PathVariable String id,
                             @AuthenticationPrincipal UserDetails principal,
                             Model model) {
        String userId = getUserId(principal);
        model.addAttribute("book", bookService.findById(id, userId));
        return "books/detail";
    }

    @GetMapping("/books/lookup")
    public String lookupPage() {
        return "books/lookup";
    }

    private String getUserId(UserDetails principal) {
        return userRepository.findByUsername(principal.getUsername())
                .map(u -> u.getId())
                .orElseThrow();
    }
}
