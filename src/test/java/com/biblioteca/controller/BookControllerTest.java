package com.biblioteca.controller;

import com.biblioteca.config.MongoTestContainer;
import com.biblioteca.model.BookStatus;
import com.biblioteca.model.User;
import com.biblioteca.repository.BookRepository;
import com.biblioteca.repository.UserRepository;
import com.biblioteca.service.AuthService;
import com.biblioteca.dto.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(MongoTestContainer.class)
@DisplayName("BookController — Testes de Caixa Preta (E2E Controller)")
class BookControllerTest {

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
        r.setUsername("testuser"); r.setEmail("test@test.com");
        r.setPassword("pass123"); r.setConfirmPassword("pass123");
        authService.register(r);
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("POST /api/books — deve criar livro e retornar 201")
    void shouldCreateBook() throws Exception {
        mockMvc.perform(post("/api/books").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Clean Code",
                                "author", "Robert Martin",
                                "status", "WANT_TO_READ"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.title").value("Clean Code"));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /api/books — deve listar livros do usuário autenticado")
    void shouldListBooks() throws Exception {
        mockMvc.perform(post("/api/books").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("title", "Livro 1", "author", "Autor", "status", "READING"))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("PUT /api/books/{id} — deve atualizar livro")
    void shouldUpdateBook() throws Exception {
        String body = mockMvc.perform(post("/api/books").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("title", "Original", "author", "Autor", "status", "WANT_TO_READ"))))
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(body).get("id").asText();

        mockMvc.perform(put("/api/books/" + id).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("title", "Atualizado", "author", "Autor", "status", "READ"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Atualizado"))
                .andExpect(jsonPath("$.status").value("READ"));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("DELETE /api/books/{id} — deve remover livro e retornar 204")
    void shouldDeleteBook() throws Exception {
        String body = mockMvc.perform(post("/api/books").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("title", "Remover", "author", "Autor", "status", "WANT_TO_READ"))))
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(body).get("id").asText();

        mockMvc.perform(delete("/api/books/" + id).with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/books/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /api/books — deve filtrar por status via query param")
    void shouldFilterByStatusParam() throws Exception {
        mockMvc.perform(post("/api/books").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("title", "Lendo", "author", "A", "status", "READING"))))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/books").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("title", "Quero", "author", "B", "status", "WANT_TO_READ"))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/books?status=reading"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Lendo"));
    }

    @Test
    @DisplayName("POST /api/books — deve retornar 401 sem autenticação")
    void shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/books").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("POST /api/books — deve retornar 400 com dados inválidos")
    void shouldReturn400WhenInvalid() throws Exception {
        mockMvc.perform(post("/api/books").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("author", "Só autor"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /api/books/stats — deve retornar estatísticas")
    void shouldReturnStats() throws Exception {
        mockMvc.perform(get("/api/books/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
    }
}
