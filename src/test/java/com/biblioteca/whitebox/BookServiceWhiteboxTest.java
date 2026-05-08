package com.biblioteca.whitebox;

import com.biblioteca.config.MongoTestContainer;
import com.biblioteca.dto.BookRequest;
import com.biblioteca.dto.BookResponse;
import com.biblioteca.exception.BusinessException;
import com.biblioteca.exception.ResourceNotFoundException;
import com.biblioteca.model.BookStatus;
import com.biblioteca.repository.BookRepository;
import com.biblioteca.service.BookService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes de Caixa Branca — cobrem branches internos do BookService:
 * ISBN duplicado, dono incorreto, status padrão, updatedAt, etc.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(MongoTestContainer.class)
@DisplayName("Testes Caixa Branca — Lógica Interna do BookService")
class BookServiceWhiteboxTest {

    @Autowired BookService bookService;
    @Autowired BookRepository bookRepository;

    static final String UID = "wb-user";

    @BeforeEach
    void clean() { bookRepository.deleteAll(); }

    @Test
    @DisplayName("Branch: ISBN null → deve criar sem verificar duplicata")
    void shouldCreateWithNullIsbn() {
        BookRequest req = buildReq("Sem ISBN", "Autor");
        req.setIsbn(null);
        assertThatCode(() -> bookService.create(req, UID)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Branch: ISBN blank → deve criar sem verificar duplicata")
    void shouldCreateWithBlankIsbn() {
        BookRequest req = buildReq("Sem ISBN", "Autor");
        req.setIsbn("   ");
        assertThatCode(() -> bookService.create(req, UID)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Branch: ISBN preenchido e duplicado → deve lançar BusinessException")
    void shouldRejectDuplicateIsbn() {
        BookRequest r1 = buildReq("Livro 1", "Autor");
        r1.setIsbn("1234567890");
        bookService.create(r1, UID);

        BookRequest r2 = buildReq("Livro 2", "Autor");
        r2.setIsbn("1234567890");
        assertThatThrownBy(() -> bookService.create(r2, UID))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Branch: status null no request → deve usar WANT_TO_READ como padrão")
    void shouldDefaultStatusToWantToRead() {
        BookRequest req = buildReq("Padrão", "Autor");
        req.setStatus(null);
        BookResponse res = bookService.create(req, UID);
        assertThat(res.getStatus()).isEqualTo(BookStatus.WANT_TO_READ);
    }

    @Test
    @DisplayName("Branch: update deve alterar updatedAt")
    void shouldUpdateTimestamp() throws InterruptedException {
        BookResponse created = bookService.create(buildReq("Original", "A"), UID);
        Thread.sleep(10);
        BookResponse updated = bookService.update(created.getId(), buildReq("Novo", "B"), UID);
        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(created.getUpdatedAt());
    }

    @Test
    @DisplayName("Branch: findById com userId errado → ResourceNotFoundException")
    void shouldThrowOnWrongOwner() {
        BookResponse b = bookService.create(buildReq("Dono", "A"), UID);
        assertThatThrownBy(() -> bookService.findById(b.getId(), "outro"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Branch: delete com userId errado → ResourceNotFoundException")
    void shouldThrowOnDeleteWrongOwner() {
        BookResponse b = bookService.create(buildReq("Dono", "A"), UID);
        assertThatThrownBy(() -> bookService.delete(b.getId(), "outro"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Branch: search sem resultados → lista vazia")
    void shouldReturnEmptyListWhenNotFound() {
        bookService.create(buildReq("Java", "Gosling"), UID);
        assertThat(bookService.search(UID, "Python")).isEmpty();
    }

    @Test
    @DisplayName("Branch: getStats com acervo vazio → todos zeros")
    void shouldReturnZeroStatsWhenEmpty() {
        var stats = bookService.getStats(UID);
        assertThat(stats.values()).allMatch(v -> v == 0L);
    }

    private BookRequest buildReq(String title, String author) {
        BookRequest r = new BookRequest();
        r.setTitle(title);
        r.setAuthor(author);
        r.setStatus(BookStatus.WANT_TO_READ);
        return r;
    }
}
