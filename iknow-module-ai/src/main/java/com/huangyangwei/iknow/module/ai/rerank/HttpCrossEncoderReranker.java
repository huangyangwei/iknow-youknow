package com.huangyangwei.iknow.module.ai.rerank;

import com.huangyangwei.iknow.module.ai.config.RagProperties;
import com.huangyangwei.iknow.module.ai.support.RetrievalCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cross-encoder reranker backed by a configurable HTTP endpoint.
 */
public class HttpCrossEncoderReranker implements Reranker {

    private static final Logger log = LoggerFactory.getLogger(HttpCrossEncoderReranker.class);

    private final URI endpoint;
    private final String model;
    private final Duration timeout;
    private final int batchSize;
    private final int topN;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public HttpCrossEncoderReranker(RagProperties.Rerank properties, ObjectMapper objectMapper) {
        this(URI.create(properties.getEndpoint()), properties.getModel(), properties.getTimeout(),
                properties.getBatchSize(), properties.getTopN(), objectMapper, HttpClient.newHttpClient());
    }

    HttpCrossEncoderReranker(URI endpoint, String model, Duration timeout, int batchSize, int topN,
                             ObjectMapper objectMapper, HttpClient httpClient) {
        this.endpoint = endpoint;
        this.model = model;
        this.timeout = timeout == null ? Duration.ofMillis(1500) : timeout;
        this.batchSize = Math.max(1, batchSize);
        this.topN = Math.max(1, topN);
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public List<RetrievalCandidate> rerank(String query, List<RetrievalCandidate> candidates) {
        if (!StringUtils.hasText(query) || candidates == null || candidates.isEmpty()) {
            return candidates == null ? List.of() : candidates;
        }
        try {
            int limit = Math.min(topN, candidates.size());
            List<RetrievalCandidate> rerankWindow = candidates.subList(0, limit);
            Map<String, Double> scores = score(query, rerankWindow);
            if (scores.isEmpty()) {
                return candidates;
            }

            List<RetrievalCandidate> scored = rerankWindow.stream()
                    .map(candidate -> candidate.withRerankScore(scores.get(candidate.candidateId())))
                    .sorted(Comparator
                            .comparing(RetrievalCandidate::rerankScore,
                                    Comparator.nullsLast(Comparator.reverseOrder()))
                            .thenComparing(RetrievalCandidate::fusionScore,
                                    Comparator.nullsLast(Comparator.reverseOrder()))
                            .thenComparing(RetrievalCandidate::candidateId, Comparator.nullsLast(String::compareTo)))
                    .toList();
            List<RetrievalCandidate> result = new ArrayList<>(candidates.size());
            result.addAll(scored);
            if (limit < candidates.size()) {
                result.addAll(candidates.subList(limit, candidates.size()));
            }
            return result;
        } catch (Exception e) {
            log.warn("reranker unavailable, falling back to fusion order: {}", e.getMessage());
            return candidates;
        }
    }

    private Map<String, Double> score(String query, List<RetrievalCandidate> candidates) throws Exception {
        Map<String, Double> scores = new HashMap<>();
        for (int start = 0; start < candidates.size(); start += batchSize) {
            int end = Math.min(start + batchSize, candidates.size());
            List<RetrievalCandidate> batch = candidates.subList(start, end);
            String body = requestBody(query, batch);
            HttpRequest request = HttpRequest.newBuilder(endpoint)
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("reranker http status " + response.statusCode());
            }
            readScores(batch, response.body(), scores);
        }
        return scores;
    }

    private String requestBody(String query, List<RetrievalCandidate> candidates) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (StringUtils.hasText(model)) {
            payload.put("model", model);
        }
        payload.put("query", query);
        payload.put("documents", candidates.stream().map(RetrievalCandidate::text).toList());
        return objectMapper.writeValueAsString(payload);
    }

    private void readScores(List<RetrievalCandidate> batch, String body, Map<String, Double> scores) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        JsonNode scoresNode = root.path("scores");
        if (scoresNode.isArray()) {
            for (int i = 0; i < Math.min(batch.size(), scoresNode.size()); i++) {
                scores.put(batch.get(i).candidateId(), scoresNode.get(i).asDouble());
            }
            return;
        }

        JsonNode resultsNode = root.path("results");
        if (resultsNode.isArray()) {
            for (JsonNode result : resultsNode) {
                int index = result.path("index").asInt(-1);
                if (index >= 0 && index < batch.size()) {
                    scores.put(batch.get(index).candidateId(), result.path("score").asDouble());
                }
            }
        }
    }
}
