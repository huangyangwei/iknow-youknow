package com.huangyangwei.iknow.module.ai.rerank;

import com.huangyangwei.iknow.module.ai.support.RetrievalCandidate;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HttpCrossEncoderRerankerTest {

    @Test
    void rerankReordersTopWindowByHttpScores() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/rerank", exchange -> {
            byte[] response = "{\"results\":[{\"index\":1,\"score\":0.95},{\"index\":0,\"score\":0.10}]}".getBytes();
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            HttpCrossEncoderReranker reranker = reranker(server, Duration.ofMillis(500), 16, 30);

            List<RetrievalCandidate> reranked = reranker.rerank("query", List.of(
                    candidate("1:1:0", "first", 0.9),
                    candidate("2:1:0", "second", 0.8)));

            assertEquals("2:1:0", reranked.get(0).candidateId());
            assertEquals(0.95, reranked.get(0).rerankScore());
            assertEquals("1:1:0", reranked.get(1).candidateId());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void rerankFallsBackToFusionOrderOnTimeout() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.setExecutor(executor);
        server.createContext("/rerank", exchange -> {
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] response = "{\"scores\":[0.1,0.9]}".getBytes();
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            HttpCrossEncoderReranker reranker = reranker(server, Duration.ofMillis(25), 16, 30);
            List<RetrievalCandidate> original = List.of(
                    candidate("1:1:0", "first", 0.9),
                    candidate("2:1:0", "second", 0.8));

            List<RetrievalCandidate> reranked = reranker.rerank("query", original);

            assertEquals(original, reranked);
        } finally {
            server.stop(0);
            executor.shutdownNow();
        }
    }

    private HttpCrossEncoderReranker reranker(HttpServer server, Duration timeout, int batchSize, int topN) {
        return new HttpCrossEncoderReranker(URI.create("http://localhost:" + server.getAddress().getPort() + "/rerank"),
                null, timeout, batchSize, topN, new ObjectMapper(), HttpClient.newHttpClient());
    }

    private RetrievalCandidate candidate(String id, String text, double fusionScore) {
        return new RetrievalCandidate(id, Long.valueOf(id.substring(0, 1)), 1, 0, "title", null, text,
                Map.of("candidateId", id), "keyword", null, null, fusionScore, null, null);
    }
}
