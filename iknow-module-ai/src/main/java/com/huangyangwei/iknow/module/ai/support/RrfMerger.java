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
                scores.merge(item, 1.0 / (K + i + 1), Double::sum);
            }
        }
        List<Ranked<T>> result = new ArrayList<>(scores.size());
        scores.forEach((item, score) -> result.add(new Ranked<>(item, score)));
        result.sort((a, b) -> Double.compare(b.score(), a.score()));
        return result;
    }

    public record Ranked<T>(T item, double score) {
    }
}
