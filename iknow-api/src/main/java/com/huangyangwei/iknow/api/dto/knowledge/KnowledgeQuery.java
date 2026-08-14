package com.huangyangwei.iknow.api.dto.knowledge;

/**
 * 知识列表查询条件（GET 绑定）。
 * sort 形如 "field[:asc|desc]"，field ∈ {updated_at, created_at, publish_time, title, view_count}，默认 updated_at desc。
 */
public class KnowledgeQuery {

    private String keyword;
    private Long categoryId;
    private Long tagId;
    private String status;
    private String knowledgeType;
    private Long page = 1L;
    private Long size = 10L;
    private String sort;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getTagId() {
        return tagId;
    }

    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getKnowledgeType() {
        return knowledgeType;
    }

    public void setKnowledgeType(String knowledgeType) {
        this.knowledgeType = knowledgeType;
    }

    public Long getPage() {
        return page;
    }

    public void setPage(Long page) {
        this.page = page;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }
}
