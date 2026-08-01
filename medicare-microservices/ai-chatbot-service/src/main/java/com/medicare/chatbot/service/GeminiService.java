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
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);
    
    // Model candidate endpoints for resilience against free-tier rate limits (429)
    private static final List<String> MODEL_NAMES = List.of(
            "gemini-3.6-flash"
    );

    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GeminiService(
            @Value("${google.gemini.api-key:}") String apiKey,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public Optional<String> generateResponse(String fullPrompt) {
        if (apiKey.isEmpty()) {
            log.warn("[GeminiService] API key is missing. Skipping external API call.");
            return Optional.empty();
        }

        // Try candidate models in order until one succeeds
        for (String modelName : MODEL_NAMES) {
            String endpointUrl = "https://generativelanguage.googleapis.com/v1beta/models/" + modelName + ":generateContent?key=";
            Optional<String> response = callGeminiEndpoint(endpointUrl, modelName, fullPrompt);
            if (response.isPresent()) {
                return response;
            }
            log.warn("[GeminiService] Model '{}' unavailable or rate limited. Trying next candidate model...", modelName);
        }

        log.error("[GeminiService] All Gemini candidate models failed or exceeded quota.");
        return Optional.empty();
    }

    private Optional<String> callGeminiEndpoint(String endpointUrl, String modelName, String fullPrompt) {
        try {
            Map<String, Object> requestPayload = Map.of(
                    "contents", List.of(
                            Map.of(
                                    "parts", List.of(
                                            Map.of("text", fullPrompt)
                                    )
                            )
                    )
            );

            String requestBody = objectMapper.writeValueAsString(requestPayload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpointUrl + apiKey))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("[GeminiService] Model '{}' call failed with status: {}. Body: {}", modelName, response.statusCode(), response.body());
                return Optional.empty();
            }

            JsonNode rootNode = objectMapper.readTree(response.body());
            String responseText = rootNode.path("candidates")
                    .path(0)
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("text")
                    .asText();

            if (responseText != null && !responseText.isBlank()) {
                log.info("[GeminiService] Successfully generated response using model '{}'", modelName);
                return Optional.of(responseText.trim());
            }

        } catch (Exception e) {
            log.error("[GeminiService] Exception during '{}' API invocation: {}", modelName, e.getMessage(), e);
        }

        return Optional.empty();
    }
}
