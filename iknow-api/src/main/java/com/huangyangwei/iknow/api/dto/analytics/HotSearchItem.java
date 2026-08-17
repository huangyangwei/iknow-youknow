package com.huangyangwei.iknow.api.dto.analytics;

import java.io.Serializable;

/**
 * 热门搜索词（Top10）：stat_query_log 中 query_type=search 按 keyword 计数。
 */
public class HotSearchItem implements Serializable {

    private String keyword;
    private long count;

    public HotSearchItem() {
    }

    public HotSearchItem(String keyword, long count) {
        this.keyword = keyword;
        this.count = count;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
