package com.biblioteca.integration;

import com.biblioteca.config.MongoTestContainer;
import com.biblioteca.dto.BookRequest;
import com.biblioteca.dto.BookResponse;
import com.biblioteca.dto.RegisterRequest;
import com.biblioteca.model.BookStatus;
import com.biblioteca.repository.BookRepository;
import com.biblioteca.repository.UserRepository;
import com.biblioteca.service.AuthService;
import com.biblioteca.service.BookService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes de Integração — fluxos completos User + Book via camada de serviço,
 * usando MongoDB real via Testcontainers (sem mocks).
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(MongoTestContainer.class)
@DisplayName("Testes de Integração — Fluxo Completo Usuário + Livros")
class BookIntegrationTest {

    @Autowired AuthService authService;
    @Autowired BookService bookService;
    @Autowired UserRepository userRepository;
    @Autowired BookRepository bookRepository;

    @BeforeEach
    void clean() {
        userRepository.deleteAll();
        bookRepository.deleteAll();
    }

    @Test
    @DisplayName("Fluxo completo: registro → adição de livros → listagem → remoção")
    void fullUserBookFlow() {
        // 1. Registrar usuário
        var user = authService.register(buildRegReq("integuser", "integ@test.com"));
        assertThat(user.getId()).isNotNull();
        String uid = user.getId();

        // 2. Adicionar livros
        bookService.create(bookReq("Livro A", "Autor A", BookStatus.WANT_TO_READ, null), uid);
        bookService.create(bookReq("Livro B", "Autor B", BookStatus.READING, null), uid);
        bookService.create(bookReq("Livro C", "Autor C", BookStatus.READ, 4), uid);

        // 3. Listar
        List<BookResponse> all = bookService.findAll(uid);
        assertThat(all).hasSize(3);

        // 4. Filtrar por status
        assertThat(bookService.findByStatus(uid, BookStatus.READ)).hasSize(1);
        assertThat(bookService.findByStatus(uid, BookStatus.READING)).hasSize(1);

        // 5. Buscar
        assertThat(bookService.search(uid, "livro")).hasSize(3);
        assertThat(bookService.search(uid, "autor b")).hasSize(1);

        // 6. Stats
        var stats = bookService.getStats(uid);
        assertThat(stats.get("total")).isEqualTo(3L);
        assertThat(stats.get("read")).isEqualTo(1L);

        // 7. Atualizar
        String id = all.get(0).getId();
        bookService.update(id, bookReq("Livro A Editado", "Autor A", BookStatus.READ, 5), uid);
        assertThat(bookService.findById(id, uid).getTitle()).isEqualTo("Livro A Editado");

        // 8. Remover
        bookService.delete(id, uid);
        assertThat(bookService.findAll(uid)).hasSize(2);
    }

    @Test
    @DisplayName("Isolamento: livros de um usuário não vazam para outro")
    void shouldIsolateBooksByUser() {
        var u1 = authService.register(buildRegReq("user1", "u1@t.com"));
        var u2 = authService.register(buildRegReq("user2", "u2@t.com"));

        bookService.create(bookReq("Livro U1", "A", BookStatus.READING, null), u1.getId());
        bookService.create(bookReq("Livro U2", "B", BookStatus.READING, null), u2.getId());

        assertThat(bookService.findAll(u1.getId())).hasSize(1);
        assertThat(bookService.findAll(u2.getId())).hasSize(1);
        assertThat(bookService.findAll(u1.getId()).get(0).getTitle()).isEqualTo("Livro U1");
    }

    private RegisterRequest buildRegReq(String username, String email) {
        RegisterRequest r = new RegisterRequest();
        r.setUsername(username); r.setEmail(email);
        r.setPassword("pass123"); r.setConfirmPassword("pass123");
        return r;
    }

    private BookRequest bookReq(String title, String author, BookStatus status, Integer rating) {
        BookRequest r = new BookRequest();
        r.setTitle(title); r.setAuthor(author);
        r.setStatus(status); r.setRating(rating);
        return r;
    }
}
