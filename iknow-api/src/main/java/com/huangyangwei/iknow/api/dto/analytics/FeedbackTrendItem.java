package com.huangyangwei.iknow.api.dto.analytics;

import java.io.Serializable;

/**
 * 反馈趋势：按天计数（YYYY-MM-DD），含当日已解决数。
 */
public class FeedbackTrendItem implements Serializable {

    private String date;
    private long count;
    private long resolvedCount;

    public FeedbackTrendItem() {
    }

    public FeedbackTrendItem(String date, long count, long resolvedCount) {
        this.date = date;
        this.count = count;
        this.resolvedCount = resolvedCount;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public long getResolvedCount() {
        return resolvedCount;
    }

    public void setResolvedCount(long resolvedCount) {
        this.resolvedCount = resolvedCount;
    }
}
