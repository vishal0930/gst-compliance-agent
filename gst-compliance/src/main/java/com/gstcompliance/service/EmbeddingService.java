package com.gstcompliance.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class EmbeddingService {

    @Value("${ollama.embedding.url:http://localhost:11434/api/embeddings}")
    private String embeddingUrl;

    @Value("${ollama.embedding.model:nomic-embed-text}")
    private String embeddingModel;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public EmbeddingService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public float[] generateEmbedding(String text) {
        try {
            log.info("🧠 Generating embedding for: {}", text.substring(0, Math.min(50, text.length())));

            String requestBody = String.format(
                    "{\"model\": \"%s\", \"prompt\": \"%s\"}",
                    embeddingModel,
                    text.replace("\"", "\\\"")
            );

            String response = restTemplate.postForObject(
                    embeddingUrl,
                    requestBody,
                    String.class
            );

            if (response == null) {
                log.error("❌ No response from Ollama embedding API");
                return new float[768];
            }

            JsonNode root = objectMapper.readTree(response);
            JsonNode embeddingNode = root.path("embedding");

            if (embeddingNode.isArray()) {
                float[] embedding = new float[embeddingNode.size()];
                for (int i = 0; i < embeddingNode.size(); i++) {
                    embedding[i] = (float) embeddingNode.get(i).asDouble();
                }
                log.info("✅ Generated embedding of size: {}", embedding.length);
                return embedding;
            }

            return new float[1536];

        } catch (Exception e) {
            log.error("❌ Failed to generate embedding: {}", e.getMessage(), e);
            return new float[768];
        }
    }

    public String embeddingToString(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}