package com.huangyangwei.iknow.module.ai.support;

import com.huangyangwei.iknow.module.ai.config.RagProperties;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContextPrunerTest {

    @Test
    void pruneRemovesDuplicateChunksMergesAdjacentChunksAndRanks() {
        RagProperties properties = new RagProperties();
        properties.getPrune().setDuplicateThreshold(0.5);
        ContextPruner pruner = new ContextPruner(properties, new ChunkTextSplitter());

        var pruned = pruner.prune(java.util.List.of(
                candidate("1:1:0", 1L, 0, "alpha beta gamma delta epsilon", 0.9),
                candidate("9:1:0", 9L, 0, "alpha beta gamma delta zeta", 0.8),
                candidate("1:1:1", 1L, 1, "next adjacent paragraph", 0.7),
                candidate("2:1:0", 2L, 0, "unique paragraph", 0.6)));

        assertEquals(2, pruned.size());
        assertEquals("1:1:0", pruned.get(0).candidateId());
        assertTrue(pruned.get(0).text().contains("next adjacent paragraph"));
        assertEquals(1, pruned.get(0).finalRank());
        assertEquals(2, pruned.get(1).finalRank());
        assertFalse(pruned.stream().anyMatch(candidate -> candidate.knowledgeId().equals(9L)));
    }

    @Test
    void pruneTruncatesToTokenBudget() {
        RagProperties properties = new RagProperties();
        properties.getPrune().setMaxContextTokens(5);
        properties.getPrune().setReservedAnswerTokens(0);
        ContextPruner pruner = new ContextPruner(properties, new ChunkTextSplitter());
        ChunkTextSplitter splitter = new ChunkTextSplitter();

        var pruned = pruner.prune(java.util.List.of(
                candidate("1:1:0", 1L, 0, "abcdefgh", 0.9),
                candidate("2:1:0", 2L, 0, "abcdefghijklmnopqrstuvwxyz", 0.8)));

        int tokens = pruned.stream().mapToInt(candidate -> splitter.estimateTokens(candidate.text())).sum();
        assertEquals(2, pruned.size());
        assertTrue(tokens <= 5, "pruned context should fit the configured token budget");
        assertTrue(pruned.get(1).text().length() < "abcdefghijklmnopqrstuvwxyz".length());
    }

    private RetrievalCandidate candidate(String id, Long knowledgeId, Integer chunkIndex, String text, double score) {
        return new RetrievalCandidate(id, knowledgeId, 1, chunkIndex, "title", null, text,
                Map.of("knowledgeId", knowledgeId, "versionNo", 1, "chunkIndex", chunkIndex),
                "keyword", null, null, score, null, null);
    }
}
