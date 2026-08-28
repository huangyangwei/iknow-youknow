package com.huangyangwei.iknow.module.ai.support;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HybridCandidateFusionTest {

    @Test
    void weightedRrfDeduplicatesCandidatesAndMarksBothSources() {
        RetrievalCandidate vectorA = candidate("1:1:0", 1L, 0, "alpha", "vector", 0.90, null);
        RetrievalCandidate vectorB = candidate("2:1:0", 2L, 0, "shared chunk", "vector", 0.80, null);
        RetrievalCandidate keywordB = candidate("2:1:0", 2L, 0, "shared chunk", "keyword", null, 0.70);
        RetrievalCandidate keywordC = candidate("3:1:0", 3L, 0, "gamma", "keyword", null, 0.50);

        var fused = HybridCandidateFusion.fuse(
                java.util.List.of(vectorA, vectorB),
                java.util.List.of(keywordB, keywordC),
                0.6,
                0.4,
                60);

        assertEquals(3, fused.size());
        assertEquals("2:1:0", fused.get(0).candidateId());
        assertEquals("both", fused.get(0).source());
        assertEquals(1.0, fused.get(0).keywordScore());
        assertEquals(0.0, fused.get(0).vectorScore());
        assertEquals(0.6 / 62.0 + 0.4 / 61.0, fused.get(0).fusionScore(), 0.000001);
        assertFalse(fused.get(0).text().contains("shared chunk\nshared chunk"));
    }

    @Test
    void normalizeScoresHandlesNullsAndFlatScores() {
        assertEquals(java.util.List.of(1.0, 0.0, 1.0),
                HybridCandidateFusion.normalizeScores(Arrays.asList(5.0, null, 5.0)));
        assertEquals(java.util.List.of(1.0, 0.0, 0.5),
                HybridCandidateFusion.normalizeScores(java.util.List.of(4.0, 2.0, 3.0)));
        assertTrue(HybridCandidateFusion.normalizeScores(java.util.List.of()).isEmpty());
    }

    private RetrievalCandidate candidate(String id, Long knowledgeId, Integer chunkIndex, String text, String source,
                                         Double vectorScore, Double keywordScore) {
        return new RetrievalCandidate(id, knowledgeId, 1, chunkIndex, "title", null, text,
                Map.of("knowledgeId", knowledgeId, "versionNo", 1, "chunkIndex", chunkIndex),
                source, vectorScore, keywordScore, null, null, null);
    }
}
