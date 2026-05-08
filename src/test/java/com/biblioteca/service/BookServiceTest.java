package com.biblioteca.service;

import com.biblioteca.config.MongoTestContainer;
import com.biblioteca.dto.BookRequest;
import com.biblioteca.dto.BookResponse;
import com.biblioteca.exception.BusinessException;
import com.biblioteca.exception.ResourceNotFoundException;
import com.biblioteca.model.BookStatus;
import com.biblioteca.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(MongoTestContainer.class)
@DisplayName("BookService — Testes de Serviço com Testcontainers")
class BookServiceTest {

    @Autowired BookService bookService;
    @Autowired BookRepository bookRepository;

    static final String USER_ID = "user-001";

    @BeforeEach
    void clean() {
        bookRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve criar livro e persistir no MongoDB")
    void shouldCreateBook() {
        BookResponse response = bookService.create(buildRequest("Clean Code", "Robert Martin"), USER_ID);
        assertThat(response.getId()).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Clean Code");
        assertThat(bookRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Deve retornar todos os livros do usuário")
    void shouldListBooks() {
        bookService.create(buildRequest("Livro A", "Autor A"), USER_ID);
        bookService.create(buildRequest("Livro B", "Autor B"), USER_ID);
        bookService.create(buildRequest("Livro C", "Autor C"), "outro-user");

        List<BookResponse> books = bookService.findAll(USER_ID);
        assertThat(books).hasSize(2);
    }

    @Test
    @DisplayName("Deve filtrar livros por status")
    void shouldFilterByStatus() {
        BookRequest req = buildRequest("Livro Status", "Autor");
        req.setStatus(BookStatus.READING);
        bookService.create(req, USER_ID);
        bookService.create(buildRequest("Quero ler", "Autor"), USER_ID);

        assertThat(bookService.findByStatus(USER_ID, BookStatus.READING)).hasSize(1);
        assertThat(bookService.findByStatus(USER_ID, BookStatus.WANT_TO_READ)).hasSize(1);
    }

    @Test
    @DisplayName("Deve buscar livros por título ou autor")
    void shouldSearch() {
        bookService.create(buildRequest("O Senhor dos Anéis", "Tolkien"), USER_ID);
        bookService.create(buildRequest("Harry Potter", "Rowling"), USER_ID);

        assertThat(bookService.search(USER_ID, "tolkien")).hasSize(1);
        assertThat(bookService.search(USER_ID, "anéis")).hasSize(1);
        assertThat(bookService.search(USER_ID, "XYZ")).isEmpty();
    }

    @Test
    @DisplayName("Deve atualizar livro existente")
    void shouldUpdateBook() {
        BookResponse created = bookService.create(buildRequest("Título Original", "Autor"), USER_ID);
        BookRequest update = buildRequest("Título Atualizado", "Novo Autor");
        update.setStatus(BookStatus.READ);
        update.setRating(5);

        BookResponse updated = bookService.update(created.getId(), update, USER_ID);
        assertThat(updated.getTitle()).isEqualTo("Título Atualizado");
        assertThat(updated.getStatus()).isEqualTo(BookStatus.READ);
        assertThat(updated.getRating()).isEqualTo(5);
    }

    @Test
    @DisplayName("Deve remover livro do usuário")
    void shouldDeleteBook() {
        BookResponse created = bookService.create(buildRequest("Para Remover", "Autor"), USER_ID);
        bookService.delete(created.getId(), USER_ID);
        assertThat(bookRepository.count()).isZero();
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar acessar livro de outro usuário")
    void shouldNotAccessOtherUsersBook() {
        BookResponse created = bookService.create(buildRequest("Livro Privado", "Autor"), USER_ID);
        assertThatThrownBy(() -> bookService.findById(created.getId(), "outro-user"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Deve rejeitar ISBN duplicado para o mesmo usuário")
    void shouldRejectDuplicateIsbn() {
        BookRequest req = buildRequest("Livro ISBN", "Autor");
        req.setIsbn("9780132350884");
        bookService.create(req, USER_ID);

        BookRequest dup = buildRequest("Outro Livro", "Outro Autor");
        dup.setIsbn("9780132350884");
        assertThatThrownBy(() -> bookService.create(dup, USER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ISBN");
    }

    @Test
    @DisplayName("Deve retornar estatísticas corretas")
    void shouldReturnStats() {
        bookService.create(buildRequest("A", "x"), USER_ID);
        BookRequest r2 = buildRequest("B", "y"); r2.setStatus(BookStatus.READING);
        bookService.create(r2, USER_ID);
        BookRequest r3 = buildRequest("C", "z"); r3.setStatus(BookStatus.READ);
        bookService.create(r3, USER_ID);

        Map<String, Long> stats = bookService.getStats(USER_ID);
        assertThat(stats.get("total")).isEqualTo(3L);
        assertThat(stats.get("reading")).isEqualTo(1L);
        assertThat(stats.get("read")).isEqualTo(1L);
        assertThat(stats.get("wantToRead")).isEqualTo(1L);
    }

    private BookRequest buildRequest(String title, String author) {
        BookRequest r = new BookRequest();
        r.setTitle(title);
        r.setAuthor(author);
        r.setStatus(BookStatus.WANT_TO_READ);
        return r;
    }
}
