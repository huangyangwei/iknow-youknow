package com.huangyangwei.iknow.module.ai.support;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Unified chunk-level retrieval candidate used by vector, FTS, rerank, pruning, and source output.
 */
public record RetrievalCandidate(String candidateId,
                                 Long knowledgeId,
                                 Integer versionNo,
                                 Integer chunkIndex,
                                 String title,
                                 String url,
                                 String text,
                                 Map<String, Object> metadata,
                                 String source,
                                 Double vectorScore,
                                 Double keywordScore,
                                 Double fusionScore,
                                 Double rerankScore,
                                 Integer finalRank) {

    public RetrievalCandidate {
        text = text == null ? "" : text;
        title = title == null ? "" : title;
        source = source == null ? "unknown" : source;
        Map<String, Object> copy = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
        metadata = Collections.unmodifiableMap(copy);
    }

    public RetrievalCandidate withSource(String source) {
        return new RetrievalCandidate(candidateId, knowledgeId, versionNo, chunkIndex, title, url, text, metadata,
                source, vectorScore, keywordScore, fusionScore, rerankScore, finalRank);
    }

    public RetrievalCandidate withVectorScore(Double vectorScore) {
        return new RetrievalCandidate(candidateId, knowledgeId, versionNo, chunkIndex, title, url, text, metadata,
                source, vectorScore, keywordScore, fusionScore, rerankScore, finalRank);
    }

    public RetrievalCandidate withKeywordScore(Double keywordScore) {
        return new RetrievalCandidate(candidateId, knowledgeId, versionNo, chunkIndex, title, url, text, metadata,
                source, vectorScore, keywordScore, fusionScore, rerankScore, finalRank);
    }

    public RetrievalCandidate withFusionScore(Double fusionScore) {
        return new RetrievalCandidate(candidateId, knowledgeId, versionNo, chunkIndex, title, url, text, metadata,
                source, vectorScore, keywordScore, fusionScore, rerankScore, finalRank);
    }

    public RetrievalCandidate withRerankScore(Double rerankScore) {
        return new RetrievalCandidate(candidateId, knowledgeId, versionNo, chunkIndex, title, url, text, metadata,
                source, vectorScore, keywordScore, fusionScore, rerankScore, finalRank);
    }

    public RetrievalCandidate withText(String text) {
        return new RetrievalCandidate(candidateId, knowledgeId, versionNo, chunkIndex, title, url, text, metadata,
                source, vectorScore, keywordScore, fusionScore, rerankScore, finalRank);
    }

    public RetrievalCandidate withFinalRank(Integer finalRank) {
        return new RetrievalCandidate(candidateId, knowledgeId, versionNo, chunkIndex, title, url, text, metadata,
                source, vectorScore, keywordScore, fusionScore, rerankScore, finalRank);
    }

    public RetrievalCandidate mergeWith(RetrievalCandidate other) {
        Map<String, Object> mergedMetadata = new LinkedHashMap<>(metadata);
        mergedMetadata.putAll(other.metadata());
        String mergedSource = mergeSource(source, other.source());
        return new RetrievalCandidate(candidateId, knowledgeId, versionNo, chunkIndex, title, url,
                mergeText(other), mergedMetadata, mergedSource, max(vectorScore, other.vectorScore()),
                max(keywordScore, other.keywordScore()), max(fusionScore, other.fusionScore()),
                max(rerankScore, other.rerankScore()), finalRank);
    }

    private String mergeText(RetrievalCandidate other) {
        if (candidateId != null && candidateId.equals(other.candidateId())) {
            return longerText(text, other.text());
        }
        return joinText(text, other.text());
    }

    private String longerText(String left, String right) {
        if (left == null || left.isBlank()) {
            return right == null ? "" : right;
        }
        if (right == null || right.isBlank()) {
            return left;
        }
        String normalizedLeft = left.replaceAll("\\s+", " ").trim();
        String normalizedRight = right.replaceAll("\\s+", " ").trim();
        if (normalizedLeft.equals(normalizedRight)) {
            return left.length() >= right.length() ? left : right;
        }
        return left.length() >= right.length() ? left : right;
    }

    private String joinText(String left, String right) {
        if (left == null || left.isBlank()) {
            return right == null ? "" : right;
        }
        if (right == null || right.isBlank()) {
            return left;
        }
        return left + "\n" + right;
    }

    private Double max(Double left, Double right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return Math.max(left, right);
    }

    public static String mergeSource(String left, String right) {
        if (left == null || left.equals(right)) {
            return right == null ? "unknown" : right;
        }
        if (right == null) {
            return left;
        }
        if ("both".equals(left) || "both".equals(right)) {
            return "both";
        }
        return "both";
    }
}
