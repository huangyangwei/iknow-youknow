package com.huangyangwei.iknow.module.ai.support;

import com.huangyangwei.iknow.module.ai.config.RagProperties;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Filters and compacts fused chunks before they enter the answer context.
 */
@Component
public class ContextPruner {

    private final RagProperties properties;
    private final ChunkTextSplitter splitter;

    public ContextPruner(RagProperties properties, ChunkTextSplitter splitter) {
        this.properties = properties;
        this.splitter = splitter;
    }

    public List<RetrievalCandidate> prune(List<RetrievalCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        if (!properties.getPrune().isEnabled()) {
            return rank(candidates);
        }
        List<RetrievalCandidate> filtered = candidates.stream()
                .filter(candidate -> score(candidate) >= properties.getPrune().getMinScore())
                .toList();
        List<RetrievalCandidate> deduped = removeDuplicates(filtered);
        List<RetrievalCandidate> merged = mergeAdjacentChunks(deduped);
        return rank(applyTokenBudget(merged));
    }

    private List<RetrievalCandidate> removeDuplicates(List<RetrievalCandidate> candidates) {
        Set<String> exactHashes = new HashSet<>();
        List<Set<String>> acceptedShingles = new ArrayList<>();
        List<RetrievalCandidate> accepted = new ArrayList<>();
        double duplicateThreshold = properties.getPrune().getDuplicateThreshold();
        for (RetrievalCandidate candidate : candidates) {
            String normalized = normalize(candidate.text());
            String hash = sha256(normalized);
            if (!exactHashes.add(hash)) {
                continue;
            }
            Set<String> shingles = shingles(normalized);
            boolean nearDuplicate = false;
            for (Set<String> previous : acceptedShingles) {
                if (jaccard(previous, shingles) >= duplicateThreshold) {
                    nearDuplicate = true;
                    break;
                }
            }
            if (!nearDuplicate) {
                accepted.add(candidate);
                acceptedShingles.add(shingles);
            }
        }
        return accepted;
    }

    private List<RetrievalCandidate> mergeAdjacentChunks(List<RetrievalCandidate> candidates) {
        List<RetrievalCandidate> merged = new ArrayList<>();
        for (RetrievalCandidate candidate : candidates) {
            if (!merged.isEmpty() && isAdjacent(merged.get(merged.size() - 1), candidate)) {
                RetrievalCandidate previous = merged.remove(merged.size() - 1);
                merged.add(previous.mergeWith(candidate));
            } else {
                merged.add(candidate);
            }
        }
        return merged;
    }

    private boolean isAdjacent(RetrievalCandidate left, RetrievalCandidate right) {
        if (left.knowledgeId() == null || right.knowledgeId() == null
                || left.versionNo() == null || right.versionNo() == null
                || left.chunkIndex() == null || right.chunkIndex() == null) {
            return false;
        }
        return left.knowledgeId().equals(right.knowledgeId())
                && left.versionNo().equals(right.versionNo())
                && Math.abs(left.chunkIndex() - right.chunkIndex()) == 1;
    }

    private List<RetrievalCandidate> applyTokenBudget(List<RetrievalCandidate> candidates) {
        int budget = Math.max(1, properties.getPrune().getMaxContextTokens()
                - Math.max(0, properties.getPrune().getReservedAnswerTokens()));
        List<RetrievalCandidate> selected = new ArrayList<>();
        int used = 0;
        for (RetrievalCandidate candidate : candidates) {
            int tokens = splitter.estimateTokens(candidate.text());
            if (used + tokens <= budget) {
                selected.add(candidate);
                used += tokens;
                continue;
            }
            int remaining = budget - used;
            if (remaining > 0) {
                String truncated = truncateToTokens(candidate.text(), remaining);
                if (!truncated.isBlank()) {
                    selected.add(candidate.withText(truncated));
                }
            }
            break;
        }
        return selected;
    }

    private String truncateToTokens(String text, int maxTokens) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            builder.append(text.charAt(i));
            if (splitter.estimateTokens(builder.toString()) > maxTokens) {
                builder.deleteCharAt(builder.length() - 1);
                break;
            }
        }
        return builder.toString().trim();
    }

    private List<RetrievalCandidate> rank(List<RetrievalCandidate> candidates) {
        List<RetrievalCandidate> ranked = new ArrayList<>(candidates.size());
        for (int i = 0; i < candidates.size(); i++) {
            ranked.add(candidates.get(i).withFinalRank(i + 1));
        }
        return ranked;
    }

    private double score(RetrievalCandidate candidate) {
        if (candidate.rerankScore() != null) {
            return candidate.rerankScore();
        }
        if (candidate.fusionScore() != null) {
            return candidate.fusionScore();
        }
        if (candidate.vectorScore() != null) {
            return candidate.vectorScore();
        }
        if (candidate.keywordScore() != null) {
            return candidate.keywordScore();
        }
        return 0;
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase().replaceAll("\\s+", " ").trim();
    }

    private String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception e) {
            return text;
        }
    }

    private Set<String> shingles(String normalized) {
        if (normalized.isBlank()) {
            return Set.of();
        }
        String[] words = normalized.split("\\s+");
        if (words.length >= 3) {
            Set<String> result = new LinkedHashSet<>();
            for (int i = 0; i <= words.length - 3; i++) {
                result.add(words[i] + " " + words[i + 1] + " " + words[i + 2]);
            }
            return result;
        }
        int size = Math.min(5, normalized.length());
        Set<String> result = new LinkedHashSet<>();
        for (int i = 0; i <= normalized.length() - size; i++) {
            result.add(normalized.substring(i, i + size));
        }
        return result;
    }

    private double jaccard(Set<String> left, Set<String> right) {
        if (left.isEmpty() && right.isEmpty()) {
            return 1;
        }
        Set<String> intersection = new HashSet<>(left);
        intersection.retainAll(right);
        Set<String> union = new HashSet<>(left);
        union.addAll(right);
        return union.isEmpty() ? 0 : (double) intersection.size() / union.size();
    }
}
