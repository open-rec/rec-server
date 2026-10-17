package com.openrec.service.embedding;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.openrec.util.JsonUtil;

import lombok.extern.slf4j.Slf4j;

/** Optional OpenAI-compatible embedding client. Failures affect only semantic recall. */
@Service
@Slf4j
public class EmbeddingService {
    private final boolean enabled;
    private final URI endpoint;
    private final String model;
    private final String apiKey;
    private final String instruction;
    private final int dimensions;
    private final int maxChars;
    private final long timeoutMillis;
    private final Semaphore permits;
    private final HttpClient client;

    public EmbeddingService(@Value("${embedding.enabled:false}") boolean enabled,
        @Value("${embedding.endpoint:http://vllm:8000/v1/embeddings}") String endpoint,
        @Value("${embedding.model:Qwen/Qwen3-Embedding-0.6B}") String model,
        @Value("${embedding.api-key:}") String apiKey,
        @Value("${embedding.instruction:Given a search query, retrieve relevant items that match the query}") String instruction,
        @Value("${embedding.dimensions:1024}") int dimensions, @Value("${embedding.timeout-ms:500}") long timeoutMillis,
        @Value("${embedding.max-concurrent:8}") int maxConcurrent, @Value("${embedding.max-chars:4096}") int maxChars) {
        this.enabled = enabled;
        this.endpoint = URI.create(endpoint);
        if (!List.of("http", "https").contains(this.endpoint.getScheme()) || dimensions <= 0 || timeoutMillis <= 0
            || maxConcurrent <= 0 || maxChars <= 0 || model.isBlank()) {
            throw new IllegalArgumentException("Invalid embedding configuration");
        }
        this.model = model;
        this.apiKey = apiKey;
        this.instruction = instruction;
        this.dimensions = dimensions;
        this.timeoutMillis = timeoutMillis;
        this.maxChars = maxChars;
        this.permits = new Semaphore(maxConcurrent);
        this.client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofMillis(timeoutMillis)).build();
    }

    public List<Double> embedQuery(String query, long budgetMillis) {
        if (!enabled || query == null || query.isBlank() || query.length() > maxChars || budgetMillis <= 0
            || !permits.tryAcquire()) {
            return Collections.emptyList();
        }
        try {
            // Match rec-algorithm's query recipe exactly; documents receive no instruction.
            String input = instruction.isBlank() ? query : "Instruct: " + instruction + "\nQuery:" + query;
            String body =
                JsonUtil.objToJson(Map.of("model", model, "input", List.of(input), "encoding_format", "float"));
            HttpRequest.Builder request =
                HttpRequest.newBuilder(endpoint).timeout(Duration.ofMillis(Math.min(timeoutMillis, budgetMillis)))
                    .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body));
            if (!apiKey.isBlank()) {
                request.header("Authorization", "Bearer " + apiKey);
            }
            HttpResponse<String> response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Embedding service returned HTTP {}", response.statusCode());
                return Collections.emptyList();
            }
            return parseVector(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Collections.emptyList();
        } catch (Exception e) {
            // Never log input text, credentials, or a provider's response body.
            log.warn("Embedding inference failed: {}", e.getClass().getSimpleName());
            return Collections.emptyList();
        } finally {
            permits.release();
        }
    }

    private List<Double> parseVector(String body) {
        JsonObject response = JsonParser.parseString(body).getAsJsonObject();
        if (response.has("model") && !model.equals(response.get("model").getAsString())) {
            return Collections.emptyList();
        }
        JsonArray data = response.getAsJsonArray("data");
        if (data == null || data.size() != 1) {
            return Collections.emptyList();
        }
        JsonArray values = data.get(0).getAsJsonObject().getAsJsonArray("embedding");
        if (values == null || values.size() != dimensions) {
            return Collections.emptyList();
        }
        List<Double> vector = new ArrayList<>();
        double norm = 0;
        for (JsonElement value : values) {
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
                return Collections.emptyList();
            }
            double number = value.getAsDouble();
            if (!Double.isFinite(number)) {
                return Collections.emptyList();
            }
            vector.add(number);
            norm += number * number;
        }
        if (!Double.isFinite(norm) || norm <= 0) {
            return Collections.emptyList();
        }
        double length = Math.sqrt(norm);
        vector.replaceAll(value -> value / length);
        return vector;
    }
}
