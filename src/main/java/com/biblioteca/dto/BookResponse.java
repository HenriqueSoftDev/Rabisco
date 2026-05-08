package com.biblioteca.dto;

import com.biblioteca.model.Book;
import com.biblioteca.model.BookStatus;

import java.time.LocalDateTime;

public class BookResponse {

    private String id;
    private String title;
    private String author;
    private String isbn;
    private String genre;
    private Integer publishedYear;
    private String synopsis;
    private String coverUrl;
    private BookStatus status;
    private Integer rating;
    private LocalDateTime addedAt;
    private LocalDateTime updatedAt;

    public static BookResponse from(Book book) {
        BookResponse r = new BookResponse();
        r.id = book.getId();
        r.title = book.getTitle();
        r.author = book.getAuthor();
        r.isbn = book.getIsbn();
        r.genre = book.getGenre();
        r.publishedYear = book.getPublishedYear();
        r.synopsis = book.getSynopsis();
        r.coverUrl = book.getCoverUrl();
        r.status = book.getStatus();
        r.rating = book.getRating();
        r.addedAt = book.getAddedAt();
        r.updatedAt = book.getUpdatedAt();
        return r;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getIsbn() { return isbn; }
    public String getGenre() { return genre; }
    public Integer getPublishedYear() { return publishedYear; }
    public String getSynopsis() { return synopsis; }
    public String getCoverUrl() { return coverUrl; }
    public BookStatus getStatus() { return status; }
    public Integer getRating() { return rating; }
    public LocalDateTime getAddedAt() { return addedAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
