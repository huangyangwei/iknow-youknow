package com.huangyangwei.iknow.module.ai.support;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Weighted reciprocal-rank fusion for vector and keyword chunk candidates.
 */
public final class HybridCandidateFusion {

    private HybridCandidateFusion() {
    }

    public static List<RetrievalCandidate> fuse(List<RetrievalCandidate> vectorCandidates,
                                                List<RetrievalCandidate> keywordCandidates,
                                                double vectorWeight,
                                                double keywordWeight,
                                                int rrfK) {
        List<RetrievalCandidate> normalizedVectors = normalizeVectorScores(vectorCandidates);
        List<RetrievalCandidate> normalizedKeywords = normalizeKeywordScores(keywordCandidates);
        Map<String, RetrievalCandidate> candidates = new LinkedHashMap<>();
        Map<String, Double> scores = new LinkedHashMap<>();

        addRanked(normalizedVectors, vectorWeight, rrfK, candidates, scores);
        addRanked(normalizedKeywords, keywordWeight, rrfK, candidates, scores);

        List<RetrievalCandidate> fused = new ArrayList<>();
        for (Map.Entry<String, RetrievalCandidate> entry : candidates.entrySet()) {
            fused.add(entry.getValue().withFusionScore(scores.get(entry.getKey())));
        }
        fused.sort(Comparator
                .comparing(RetrievalCandidate::fusionScore, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(RetrievalCandidate::candidateId, Comparator.nullsLast(String::compareTo)));
        return fused;
    }

    public static List<Double> normalizeScores(List<Double> scores) {
        if (scores == null || scores.isEmpty()) {
            return List.of();
        }
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (Double score : scores) {
            if (score == null || score.isNaN()) {
                continue;
            }
            min = Math.min(min, score);
            max = Math.max(max, score);
        }
        if (min == Double.POSITIVE_INFINITY) {
            return scores.stream().map(score -> 0.0).toList();
        }
        if (Double.compare(min, max) == 0) {
            return scores.stream().map(score -> score == null || score.isNaN() ? 0.0 : 1.0).toList();
        }
        double minScore = min;
        double range = max - min;
        return scores.stream()
                .map(score -> score == null || score.isNaN() ? 0.0 : (score - minScore) / range)
                .toList();
    }

    private static List<RetrievalCandidate> normalizeVectorScores(List<RetrievalCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        List<Double> normalized = normalizeScores(candidates.stream().map(RetrievalCandidate::vectorScore).toList());
        List<RetrievalCandidate> result = new ArrayList<>(candidates.size());
        for (int i = 0; i < candidates.size(); i++) {
            result.add(candidates.get(i).withVectorScore(normalized.get(i)));
        }
        return result;
    }

    private static List<RetrievalCandidate> normalizeKeywordScores(List<RetrievalCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        List<Double> normalized = normalizeScores(candidates.stream().map(RetrievalCandidate::keywordScore).toList());
        List<RetrievalCandidate> result = new ArrayList<>(candidates.size());
        for (int i = 0; i < candidates.size(); i++) {
            result.add(candidates.get(i).withKeywordScore(normalized.get(i)));
        }
        return result;
    }

    private static void addRanked(List<RetrievalCandidate> ranked,
                                  double weight,
                                  int rrfK,
                                  Map<String, RetrievalCandidate> candidates,
                                  Map<String, Double> scores) {
        if (ranked == null || ranked.isEmpty() || weight <= 0) {
            return;
        }
        int k = Math.max(1, rrfK);
        for (int i = 0; i < ranked.size(); i++) {
            RetrievalCandidate candidate = ranked.get(i);
            candidates.merge(candidate.candidateId(), candidate, RetrievalCandidate::mergeWith);
            scores.merge(candidate.candidateId(), RrfMerger.score(i, weight, k), Double::sum);
        }
    }
}
