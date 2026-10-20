package com.company.incidentportal.ai;

import com.company.incidentportal.exception.AiUnavailableException;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Thin wrapper around the Claude Messages API. The API key comes from configuration
 * (env ANTHROPIC_API_KEY) and is only ever used server-side.
 */
@Component
public class ClaudeClient {

    private static final Logger log = LoggerFactory.getLogger(ClaudeClient.class);

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public ClaudeClient(@Value("${app.ai.base-url:https://api.anthropic.com}") String baseUrl,
                        @Value("${app.ai.api-key:}") String apiKey,
                        @Value("${app.ai.model:claude-haiku-5-5}") String model,
                        @Value("${app.ai.timeout-seconds:30}") int timeoutSeconds) {
        this.apiKey = apiKey;
        this.model = model;

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader("anthropic-version", "2023-06-01")
                .build();
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /** Sends one prompt and returns the model's text answer. */
    public String complete(String system, String user, int maxTokens) {
        if (!isConfigured()) {
            throw new AiUnavailableException("AI features are not configured (set ANTHROPIC_API_KEY)");
        }

        Map<String, Object> body = Map.of(
                "model", model,
                "max_tokens", maxTokens,
                "system", system,
                "messages", List.of(Map.of("role", "user", "content", user)));

        try {
            JsonNode response = restClient.post()
                    .uri("/v1/messages")
                    .header("x-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            StringBuilder text = new StringBuilder();
            if (response != null) {
                for (JsonNode block : response.path("content")) {
                    if ("text".equals(block.path("type").asText())) {
                        text.append(block.path("text").asText());
                    }
                }
            }
            if (text.isEmpty()) {
                throw new AiUnavailableException("The AI service returned an empty answer");
            }
            return text.toString();
        } catch (RestClientException ex) {
            log.warn("Claude API call failed: {}", ex.getMessage());
            throw new AiUnavailableException("The AI service is temporarily unavailable");
        }
    }
}
