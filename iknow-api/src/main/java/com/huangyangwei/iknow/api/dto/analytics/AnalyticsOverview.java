package com.huangyangwei.iknow.api.dto.analytics;

import java.io.Serializable;

/**
 * 仪表盘核心指标卡（技术方案 §7.2 overview）。
 * noResultRate/adoptionRate 为 0~1 小数。
 */
public class AnalyticsOverview implements Serializable {

    private long knowledgeCount;
    private long categoryCount;
    private long feedbackCount;
    private long pendingFeedbackCount;
    private long queryCount;
    private long searchCount;
    private long qaCount;
    private double noResultRate;
    private double adoptionRate;
    private long likeCount;
    private long dislikeCount;

    public long getKnowledgeCount() {
        return knowledgeCount;
    }

    public void setKnowledgeCount(long knowledgeCount) {
        this.knowledgeCount = knowledgeCount;
    }

    public long getCategoryCount() {
        return categoryCount;
    }

    public void setCategoryCount(long categoryCount) {
        this.categoryCount = categoryCount;
    }

    public long getFeedbackCount() {
        return feedbackCount;
    }

    public void setFeedbackCount(long feedbackCount) {
        this.feedbackCount = feedbackCount;
    }

    public long getPendingFeedbackCount() {
        return pendingFeedbackCount;
    }

    public void setPendingFeedbackCount(long pendingFeedbackCount) {
        this.pendingFeedbackCount = pendingFeedbackCount;
    }

    public long getQueryCount() {
        return queryCount;
    }

    public void setQueryCount(long queryCount) {
        this.queryCount = queryCount;
    }

    public long getSearchCount() {
        return searchCount;
    }

    public void setSearchCount(long searchCount) {
        this.searchCount = searchCount;
    }

    public long getQaCount() {
        return qaCount;
    }

    public void setQaCount(long qaCount) {
        this.qaCount = qaCount;
    }

    public double getNoResultRate() {
        return noResultRate;
    }

    public void setNoResultRate(double noResultRate) {
        this.noResultRate = noResultRate;
    }

    public double getAdoptionRate() {
        return adoptionRate;
    }

    public void setAdoptionRate(double adoptionRate) {
        this.adoptionRate = adoptionRate;
    }

    public long getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(long likeCount) {
        this.likeCount = likeCount;
    }

    public long getDislikeCount() {
        return dislikeCount;
    }

    public void setDislikeCount(long dislikeCount) {
        this.dislikeCount = dislikeCount;
    }
}
