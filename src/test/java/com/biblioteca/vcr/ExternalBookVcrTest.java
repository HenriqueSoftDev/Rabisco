package com.biblioteca.vcr;

import com.biblioteca.dto.ExternalBookInfo;
import com.biblioteca.service.ExternalBookService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.*;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Testes VCR (cassetes WireMock) para ExternalBookService.
 * Nenhuma chamada real à internet — as respostas vêm de stubs JSON gravados em
 * src/test/resources/wiremock/.
 */
@DisplayName("VCR — ExternalBookService com WireMock")
class ExternalBookVcrTest {

    static WireMockServer wireMock;
    ExternalBookService service;

    @BeforeAll
    static void startServer() {
        wireMock = new WireMockServer(
                WireMockConfiguration.options()
                        .port(0)
                        .usingFilesUnderClasspath("wiremock")
        );
        wireMock.start();
    }

    @AfterAll
    static void stopServer() {
        wireMock.stop();
    }

    @BeforeEach
    void setup() {
        service = new ExternalBookService(
                new RestTemplate(),
                new ObjectMapper(),
                "http://localhost:" + wireMock.port()
        );
        wireMock.resetAll();
    }

    @Test
    @DisplayName("VCR: ISBN conhecido → deve retornar dados do cassete")
    void shouldReturnBookFromCassette() {
        wireMock.stubFor(get(urlPathEqualTo("/api/books"))
                .withQueryParam("bibkeys", equalTo("ISBN:9780132350884"))
                .withQueryParam("format", equalTo("json"))
                .withQueryParam("jscmd", equalTo("data"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("open-library-clean-code.json")));

        Optional<ExternalBookInfo> result = service.lookupByIsbn("9780132350884");

        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("Clean Code");
        assertThat(result.get().getAuthor()).isEqualTo("Robert C. Martin");
        assertThat(result.get().getPublishedYear()).isEqualTo(2008);
        assertThat(result.get().getCoverUrl()).contains("openlibrary.org");
        assertThat(result.get().getNumberOfPages()).isEqualTo(431);
    }

    @Test
    @DisplayName("VCR: ISBN desconhecido → deve retornar Optional vazio")
    void shouldReturnEmptyForUnknownIsbn() {
        wireMock.stubFor(get(urlPathEqualTo("/api/books"))
                .withQueryParam("bibkeys", equalTo("ISBN:0000000000000"))
                .withQueryParam("format", equalTo("json"))
                .withQueryParam("jscmd", equalTo("data"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{}")));

        Optional<ExternalBookInfo> result = service.lookupByIsbn("0000000000000");
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("VCR: servidor retorna 500 → deve retornar Optional vazio sem lançar exceção")
    void shouldHandleServerError() {
        wireMock.stubFor(get(urlPathEqualTo("/api/books"))
                .willReturn(aResponse().withStatus(500)));

        assertThatCode(() -> service.lookupByIsbn("9999999999999")).doesNotThrowAnyException();
        assertThat(service.lookupByIsbn("9999999999999")).isEmpty();
    }

    @Test
    @DisplayName("VCR: resposta com JSON malformado → deve retornar Optional vazio")
    void shouldHandleMalformedJson() {
        wireMock.stubFor(get(urlPathEqualTo("/api/books"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("NOT_JSON")));

        assertThat(service.lookupByIsbn("1111111111111")).isEmpty();
    }

    @Test
    @DisplayName("VCR: verifica que WireMock foi chamado exatamente uma vez")
    void shouldCallExternalApiOnce() {
        wireMock.stubFor(get(urlPathEqualTo("/api/books"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("open-library-clean-code.json")));

        service.lookupByIsbn("9780132350884");

        wireMock.verify(1, getRequestedFor(urlPathEqualTo("/api/books")));
    }
}
