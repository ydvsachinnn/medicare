package com.medicare.chatbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);
    private static final String EMBED_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:embedContent?key=";

    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public EmbeddingService(
            @Value("${google.gemini.api-key:}") String apiKey,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public List<Double> getEmbedding(String text) {
        if (apiKey.isEmpty()) {
            log.warn("[EmbeddingService] API key is missing. Skipping embedding generation.");
            return Collections.emptyList();
        }

        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            Map<String, Object> requestPayload = Map.of(
                    "model", "models/gemini-embedding-001",
                    "content", Map.of(
                            "parts", List.of(
                                    Map.of("text", text)
                            )
                    )
            );

            String requestBody = objectMapper.writeValueAsString(requestPayload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(EMBED_API_URL + apiKey))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("[EmbeddingService] Call failed with status: {}. Body: {}", response.statusCode(), response.body());
                return Collections.emptyList();
            }

            JsonNode rootNode = objectMapper.readTree(response.body());
            JsonNode valuesNode = rootNode.path("embedding").path("values");

            if (valuesNode.isArray()) {
                List<Double> embedding = new ArrayList<>();
                for (JsonNode val : valuesNode) {
                    embedding.add(val.asDouble());
                }
                return embedding;
            }

        } catch (Exception e) {
            log.error("[EmbeddingService] Error creating vector embedding: {}", e.getMessage(), e);
        }

        return Collections.emptyList();
    }
}
