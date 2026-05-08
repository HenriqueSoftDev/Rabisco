package com.biblioteca.parameterized;

import com.biblioteca.config.MongoTestContainer;
import com.biblioteca.dto.BookRequest;
import com.biblioteca.model.BookStatus;
import com.biblioteca.repository.BookRepository;
import com.biblioteca.repository.UserRepository;
import com.biblioteca.service.AuthService;
import com.biblioteca.dto.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.stream.Stream;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(MongoTestContainer.class)
@DisplayName("Testes Parametrizados — Validação de Livro")
class BookValidationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired BookRepository bookRepository;
    @Autowired UserRepository userRepository;
    @Autowired AuthService authService;

    @BeforeEach
    void setup() {
        bookRepository.deleteAll();
        userRepository.deleteAll();
        RegisterRequest r = new RegisterRequest();
        r.setUsername("testuser"); r.setEmail("t@t.com");
        r.setPassword("pass123"); r.setConfirmPassword("pass123");
        authService.register(r);
    }

    @ParameterizedTest(name = "Título inválido: [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @WithMockUser(username = "testuser")
    @DisplayName("Deve rejeitar título vazio ou nulo")
    void shouldRejectInvalidTitle(String title) throws Exception {
        BookRequest req = new BookRequest();
        req.setTitle(title);
        req.setAuthor("Autor");
        req.setStatus(BookStatus.WANT_TO_READ);

        mockMvc.perform(post("/api/books").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest(name = "Ano inválido: [{0}]")
    @ValueSource(ints = {999, 2101, 0, -1})
    @WithMockUser(username = "testuser")
    @DisplayName("Deve rejeitar ano de publicação fora do intervalo válido")
    void shouldRejectInvalidYear(int year) throws Exception {
        BookRequest req = new BookRequest();
        req.setTitle("Título");
        req.setAuthor("Autor");
        req.setPublishedYear(year);
        req.setStatus(BookStatus.WANT_TO_READ);

        mockMvc.perform(post("/api/books").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest(name = "Avaliação inválida: [{0}]")
    @ValueSource(ints = {0, 6, -1, 10})
    @WithMockUser(username = "testuser")
    @DisplayName("Deve rejeitar avaliação fora do range 1-5")
    void shouldRejectInvalidRating(int rating) throws Exception {
        BookRequest req = new BookRequest();
        req.setTitle("Título");
        req.setAuthor("Autor");
        req.setRating(rating);
        req.setStatus(BookStatus.WANT_TO_READ);

        mockMvc.perform(post("/api/books").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest(name = "Livro válido: título=[{0}], autor=[{1}]")
    @MethodSource("validBooksProvider")
    @WithMockUser(username = "testuser")
    @DisplayName("Deve aceitar livros com dados válidos")
    void shouldAcceptValidBooks(String title, String author, BookStatus status) throws Exception {
        BookRequest req = new BookRequest();
        req.setTitle(title);
        req.setAuthor(author);
        req.setStatus(status);

        mockMvc.perform(post("/api/books").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        bookRepository.deleteAll();
    }

    static Stream<Arguments> validBooksProvider() {
        return Stream.of(
                Arguments.of("Dom Quixote", "Cervantes", BookStatus.READ),
                Arguments.of("1984", "Orwell", BookStatus.READING),
                Arguments.of("O Hobbit", "Tolkien", BookStatus.WANT_TO_READ),
                Arguments.of("A", "B", BookStatus.WANT_TO_READ)
        );
    }

    @ParameterizedTest(name = "Registro inválido: username=[{0}], email=[{1}]")
    @CsvSource({
            "'', teste@test.com",
            "ab, teste@test.com",
            "validuser, nao-e-email"
    })
    @DisplayName("Deve rejeitar cadastros de usuário com dados inválidos")
    void shouldRejectInvalidRegistration(String username, String email) throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", username)
                        .param("email", email)
                        .param("password", "pass123")
                        .param("confirmPassword", "pass123"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }
}
