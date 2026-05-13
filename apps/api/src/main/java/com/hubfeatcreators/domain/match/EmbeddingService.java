package com.hubfeatcreators.domain.match;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);
    private static final int DIMS = 384;
    private static final float[] ZERO_VECTOR = new float[DIMS];

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper;
    private final String aiWorkerUrl;

    public EmbeddingService(
            ObjectMapper mapper,
            @Value("${app.ai-worker.url:}") String aiWorkerUrl) {
        this.mapper = mapper;
        this.aiWorkerUrl = aiWorkerUrl;
    }

    public float[] embed(String text) {
        if (aiWorkerUrl.isBlank() || text == null || text.isBlank()) {
            return ZERO_VECTOR;
        }
        try {
            String body = mapper.writeValueAsString(new EmbedRequest(text));
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(aiWorkerUrl + "/embed"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) {
                log.warn("ai-worker /embed returned {}", res.statusCode());
                return ZERO_VECTOR;
            }
            JsonNode node = mapper.readTree(res.body());
            JsonNode embedding = node.get("embedding");
            float[] result = new float[DIMS];
            for (int i = 0; i < DIMS && i < embedding.size(); i++) {
                result[i] = (float) embedding.get(i).asDouble();
            }
            return result;
        } catch (Exception e) {
            log.warn("Embedding failed, using zero vector: {}", e.getMessage());
            return ZERO_VECTOR;
        }
    }

    private record EmbedRequest(String text) {}
}
