package com.huangyangwei.iknow.module.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Type-safe RAG enhancement switches and tuning parameters.
 */
@ConfigurationProperties(prefix = "iknow.rag")
public class RagProperties {

    private Enhanced enhanced = new Enhanced();
    private Vector vector = new Vector();
    private Keyword keyword = new Keyword();
    private Fusion fusion = new Fusion();
    private Rerank rerank = new Rerank();
    private Prune prune = new Prune();

    public Enhanced getEnhanced() {
        return enhanced;
    }

    public void setEnhanced(Enhanced enhanced) {
        this.enhanced = enhanced;
    }

    public Vector getVector() {
        return vector;
    }

    public void setVector(Vector vector) {
        this.vector = vector;
    }

    public Keyword getKeyword() {
        return keyword;
    }

    public void setKeyword(Keyword keyword) {
        this.keyword = keyword;
    }

    public Fusion getFusion() {
        return fusion;
    }

    public void setFusion(Fusion fusion) {
        this.fusion = fusion;
    }

    public Rerank getRerank() {
        return rerank;
    }

    public void setRerank(Rerank rerank) {
        this.rerank = rerank;
    }

    public Prune getPrune() {
        return prune;
    }

    public void setPrune(Prune prune) {
        this.prune = prune;
    }

    public static class Enhanced {

        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class Vector {

        private int topK = 30;

        public int getTopK() {
            return topK;
        }

        public void setTopK(int topK) {
            this.topK = topK;
        }
    }

    public static class Keyword {

        private int topK = 20;

        public int getTopK() {
            return topK;
        }

        public void setTopK(int topK) {
            this.topK = topK;
        }
    }

    public static class Fusion {

        private double vectorWeight = 0.6;
        private double keywordWeight = 0.4;
        private int rrfK = 60;

        public double getVectorWeight() {
            return vectorWeight;
        }

        public void setVectorWeight(double vectorWeight) {
            this.vectorWeight = vectorWeight;
        }

        public double getKeywordWeight() {
            return keywordWeight;
        }

        public void setKeywordWeight(double keywordWeight) {
            this.keywordWeight = keywordWeight;
        }

        public int getRrfK() {
            return rrfK;
        }

        public void setRrfK(int rrfK) {
            this.rrfK = rrfK;
        }
    }

    public static class Rerank {

        private boolean enabled = false;
        private String endpoint;
        private String model;
        private Duration timeout = Duration.ofMillis(1500);
        private int batchSize = 16;
        private int topN = 30;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        public int getTopN() {
            return topN;
        }

        public void setTopN(int topN) {
            this.topN = topN;
        }
    }

    public static class Prune {

        private boolean enabled = true;
        private int maxContextTokens = 3000;
        private int reservedAnswerTokens = 800;
        private double minScore = 0.0;
        private double duplicateThreshold = 0.92;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMaxContextTokens() {
            return maxContextTokens;
        }

        public void setMaxContextTokens(int maxContextTokens) {
            this.maxContextTokens = maxContextTokens;
        }

        public int getReservedAnswerTokens() {
            return reservedAnswerTokens;
        }

        public void setReservedAnswerTokens(int reservedAnswerTokens) {
            this.reservedAnswerTokens = reservedAnswerTokens;
        }

        public double getMinScore() {
            return minScore;
        }

        public void setMinScore(double minScore) {
            this.minScore = minScore;
        }

        public double getDuplicateThreshold() {
            return duplicateThreshold;
        }

        public void setDuplicateThreshold(double duplicateThreshold) {
            this.duplicateThreshold = duplicateThreshold;
        }
    }
}
