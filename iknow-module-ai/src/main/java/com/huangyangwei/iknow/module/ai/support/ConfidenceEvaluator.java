package com.huangyangwei.iknow.module.ai.support;

/**
 * 置信度评估（技术方案 §5.4）：
 * score = 0.4×检索相似度 + 0.3×引用覆盖率 + 0.3×LLM 自评。
 * 级别：无引用强制 low；score≥0.75 且引用数≥2 为 high；score≥0.5 为 medium；否则 low。
 */
public final class ConfidenceEvaluator {

    private ConfidenceEvaluator() {
    }

    public record Confidence(double score, String level) {
    }

    public static Confidence evaluate(double retrievalSimilarity, double citationCoverage,
                                      double llmSelfEval, int citationCount) {
        double score = 0.4 * clamp01(retrievalSimilarity)
                + 0.3 * clamp01(citationCoverage)
                + 0.3 * clamp01(llmSelfEval);
        String level;
        if (citationCount <= 0) {
            level = "low";
        } else if (score >= 0.75 && citationCount >= 2) {
            level = "high";
        } else if (score >= 0.5) {
            level = "medium";
        } else {
            level = "low";
        }
        return new Confidence(score, level);
    }

    private static double clamp01(double v) {
        if (Double.isNaN(v)) {
            return 0;
        }
        return Math.max(0, Math.min(1, v));
    }
}
