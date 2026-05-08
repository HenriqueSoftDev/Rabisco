package com.biblioteca.service;

import com.biblioteca.config.MongoTestContainer;
import com.biblioteca.dto.RegisterRequest;
import com.biblioteca.exception.BusinessException;
import com.biblioteca.model.User;
import com.biblioteca.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(MongoTestContainer.class)
@DisplayName("AuthService — Testes de Serviço com Testcontainers")
class AuthServiceTest {

    @Autowired AuthService authService;
    @Autowired UserRepository userRepository;

    @BeforeEach
    void clean() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve registrar usuário com dados válidos")
    void shouldRegisterUser() {
        RegisterRequest req = buildRequest("alice", "alice@test.com", "secret123", "secret123");
        User saved = authService.register(req);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUsername()).isEqualTo("alice");
        assertThat(saved.getPassword()).doesNotContain("secret123"); // deve estar encoded
    }

    @Test
    @DisplayName("Deve lançar exceção quando senhas não coincidem")
    void shouldFailWhenPasswordsMismatch() {
        RegisterRequest req = buildRequest("bob", "bob@test.com", "pass1", "pass2");
        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Senhas não coincidem");
    }

    @Test
    @DisplayName("Deve lançar exceção quando username já existe")
    void shouldFailOnDuplicateUsername() {
        authService.register(buildRequest("carol", "carol@test.com", "pass123", "pass123"));
        assertThatThrownBy(() -> authService.register(
                buildRequest("carol", "carol2@test.com", "pass123", "pass123")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Username");
    }

    @Test
    @DisplayName("Deve lançar exceção quando email já existe")
    void shouldFailOnDuplicateEmail() {
        authService.register(buildRequest("dave", "dave@test.com", "pass123", "pass123"));
        assertThatThrownBy(() -> authService.register(
                buildRequest("dave2", "dave@test.com", "pass123", "pass123")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Email");
    }

    @Test
    @DisplayName("Deve persistir usuário no MongoDB via Testcontainers")
    void shouldPersistInRealMongoDB() {
        authService.register(buildRequest("eve", "eve@test.com", "pass123", "pass123"));
        assertThat(userRepository.findByUsername("eve")).isPresent();
        assertThat(userRepository.count()).isEqualTo(1);
    }

    private RegisterRequest buildRequest(String user, String email, String pass, String confirm) {
        RegisterRequest r = new RegisterRequest();
        r.setUsername(user);
        r.setEmail(email);
        r.setPassword(pass);
        r.setConfirmPassword(confirm);
        return r;
    }
}
