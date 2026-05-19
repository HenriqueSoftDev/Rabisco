package com.biblioteca.controller;

import com.biblioteca.dto.ExternalBookInfo;
import com.biblioteca.service.ExternalBookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/external")
public class ExternalBookController {

    private final ExternalBookService externalBookService;

    public ExternalBookController(ExternalBookService externalBookService) {
        this.externalBookService = externalBookService;
    }

    @GetMapping("/lookup")
    public ResponseEntity<ExternalBookInfo> lookup(@RequestParam String isbn) {
        // Valida formato do ISBN (somente dígitos, X e hífens) antes de usar em URL externa
        if (isbn == null || !isbn.matches("[0-9X\\-]{10,17}")) {
            return ResponseEntity.badRequest().build();
        }
        return externalBookService.lookupByIsbn(isbn)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
