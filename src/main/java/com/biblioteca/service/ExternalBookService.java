package com.biblioteca.service;

import com.biblioteca.dto.ExternalBookInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Service
public class ExternalBookService {

    private static final Logger log = LoggerFactory.getLogger(ExternalBookService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public ExternalBookService(RestTemplate restTemplate,
                               ObjectMapper objectMapper,
                               @Value("${external.openlibrary.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
    }

    public Optional<ExternalBookInfo> lookupByIsbn(String isbn) {
        // Sanitiza o ISBN para log (previne log injection com dados controlados pelo usuário)
        String safeIsbn = isbn.replaceAll("[^0-9X\\-]", "");
        String url = baseUrl + "/api/books?bibkeys=ISBN:" + safeIsbn + "&format=json&jscmd=data";
        log.info("Consultando Open Library para ISBN {}: {}", safeIsbn, url); // NOSONAR: isbn sanitizado antes do log
        try {
            String response = restTemplate.getForObject(url, String.class);
            log.info("Resposta Open Library (ISBN {}): {}", safeIsbn, response); // NOSONAR: isbn sanitizado antes do log
            if (response == null || response.isBlank() || response.equals("{}")) {
                log.warn("ISBN {} não encontrado na Open Library (resposta vazia)", safeIsbn); // NOSONAR: isbn sanitizado antes do log
                return Optional.empty();
            }
            return parseOpenLibraryResponse(safeIsbn, response);
        } catch (Exception e) {
            log.error("Erro ao consultar Open Library para ISBN {}: {}", safeIsbn, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<ExternalBookInfo> parseOpenLibraryResponse(String isbn, String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            String key = "ISBN:" + isbn;
            JsonNode bookNode = root.get(key);
            if (bookNode == null) return Optional.empty();

            ExternalBookInfo info = new ExternalBookInfo();
            info.setIsbn(isbn);

            if (bookNode.has("title")) {
                info.setTitle(bookNode.get("title").asText());
            }
            if (bookNode.has("authors") && bookNode.get("authors").isArray()) {
                JsonNode firstAuthor = bookNode.get("authors").get(0);
                if (firstAuthor != null && firstAuthor.has("name")) {
                    info.setAuthor(firstAuthor.get("name").asText());
                }
            }
            if (bookNode.has("publishers") && bookNode.get("publishers").isArray()) {
                JsonNode firstPublisher = bookNode.get("publishers").get(0);
                if (firstPublisher != null && firstPublisher.has("name")) {
                    info.setPublisher(firstPublisher.get("name").asText());
                }
            }
            if (bookNode.has("publish_date")) {
                String dateStr = bookNode.get("publish_date").asText();
                extractYear(dateStr).ifPresent(info::setPublishedYear);
            }
            if (bookNode.has("notes")) {
                info.setSynopsis(bookNode.get("notes").asText());
            }
            if (bookNode.has("cover") && bookNode.get("cover").has("large")) {
                info.setCoverUrl(bookNode.get("cover").get("large").asText());
            }
            if (bookNode.has("number_of_pages")) {
                info.setNumberOfPages(bookNode.get("number_of_pages").asInt());
            }
            return Optional.of(info);
        } catch (Exception e) {
            log.error("Erro ao parsear resposta da Open Library (ISBN {}): {}", isbn, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<Integer> extractYear(String dateStr) {
        if (dateStr == null) return Optional.empty();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d{4}").matcher(dateStr);
        if (m.find()) {
            return Optional.of(Integer.parseInt(m.group()));
        }
        return Optional.empty();
    }
}
