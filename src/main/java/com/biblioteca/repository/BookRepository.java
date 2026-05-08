package com.biblioteca.repository;

import com.biblioteca.model.Book;
import com.biblioteca.model.BookStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BookRepository extends MongoRepository<Book, String> {
    List<Book> findByUserId(String userId);
    List<Book> findByUserIdAndStatus(String userId, BookStatus status);
    Optional<Book> findByIdAndUserId(String id, String userId);

    @Query("{ 'userId': ?0, $or: [ { 'title': { $regex: ?1, $options: 'i' } }, { 'author': { $regex: ?1, $options: 'i' } } ] }")
    List<Book> searchByUserIdAndQuery(String userId, String query);

    boolean existsByIsbnAndUserId(String isbn, String userId);
    long countByUserId(String userId);
    long countByUserIdAndStatus(String userId, BookStatus status);
}
