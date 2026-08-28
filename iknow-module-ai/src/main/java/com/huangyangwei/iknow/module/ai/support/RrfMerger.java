package com.huangyangwei.iknow.module.ai.support;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RRF（Reciprocal Rank Fusion，技术方案 §5.2）：多路召回按 rank 融合，
 * score = Σ 1/(k + rank)，k=60。输入为已按相关性排序的条目列表，按业务 id 去重聚合。
 */
public final class RrfMerger {

    private static final int K = 60;

    private RrfMerger() {
    }

    /** 融合多路排序列表，返回按融合分降序的去重条目。 */
    public static <T> List<Ranked<T>> merge(List<List<T>> rankedLists) {
        Map<T, Double> scores = new LinkedHashMap<>();
        for (List<T> ranked : rankedLists) {
            if (ranked == null) {
                continue;
            }
            for (int i = 0; i < ranked.size(); i++) {
                T item = ranked.get(i);
                scores.merge(item, score(i, 1.0, K), Double::sum);
            }
        }
        return sort(scores);
    }

    public static <T> List<Ranked<T>> mergeWeighted(List<WeightedRankedList<T>> rankedLists, int rrfK) {
        Map<T, Double> scores = new LinkedHashMap<>();
        for (WeightedRankedList<T> ranked : rankedLists) {
            if (ranked == null || ranked.items() == null || ranked.weight() <= 0) {
                continue;
            }
            for (int i = 0; i < ranked.items().size(); i++) {
                T item = ranked.items().get(i);
                scores.merge(item, score(i, ranked.weight(), rrfK), Double::sum);
            }
        }
        return sort(scores);
    }

    public static double score(int zeroBasedRank, double weight, int rrfK) {
        return weight / (Math.max(1, rrfK) + zeroBasedRank + 1.0);
    }

    private static <T> List<Ranked<T>> sort(Map<T, Double> scores) {
        List<Ranked<T>> result = new ArrayList<>(scores.size());
        scores.forEach((item, score) -> result.add(new Ranked<>(item, score)));
        result.sort((a, b) -> Double.compare(b.score(), a.score()));
        return result;
    }

    public record WeightedRankedList<T>(List<T> items, double weight) {
    }

    public record Ranked<T>(T item, double score) {
    }
}
