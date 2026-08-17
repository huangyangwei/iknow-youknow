package com.huangyangwei.iknow.api.dto.analytics;

import java.io.Serializable;

/**
 * 分类分布：kb_knowledge（published）按 category_id 计数。
 */
public class CategoryDistributionItem implements Serializable {

    private Long categoryId;
    private String categoryName;
    private long count;

    public CategoryDistributionItem() {
    }

    public CategoryDistributionItem(Long categoryId, String categoryName, long count) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.count = count;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
