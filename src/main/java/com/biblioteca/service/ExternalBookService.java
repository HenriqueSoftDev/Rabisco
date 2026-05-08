package com.biblioteca.service;

import com.biblioteca.dto.ExternalBookInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Service
public class ExternalBookService {

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
        String url = baseUrl + "/api/books?bibkeys=ISBN:" + isbn + "&format=json&jscmd=data";
        try {
            String response = restTemplate.getForObject(url, String.class);
            if (response == null || response.isBlank() || response.equals("{}")) {
                return Optional.empty();
            }
            return parseOpenLibraryResponse(isbn, response);
        } catch (Exception e) {
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
