package com.huangyangwei.iknow.api.dto.knowledge;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识条目更新请求。传 null 的字段保持原值；htmlContent 变更时自动重算 plain_text。
 */
public class KnowledgeUpdateRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 255, message = "标题最长 255 字符")
    private String title;

    private String htmlContent;

    @Size(max = 500, message = "摘要最长 500 字符")
    private String summary;

    private Long categoryId;

    @Size(max = 32, message = "知识类型最长 32 字符")
    private String knowledgeType;

    private List<Long> tagIds;

    private LocalDateTime scheduledPublishTime;

    @Size(max = 500, message = "变更说明最长 500 字符")
    private String changeNote;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getHtmlContent() {
        return htmlContent;
    }

    public void setHtmlContent(String htmlContent) {
        this.htmlContent = htmlContent;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getKnowledgeType() {
        return knowledgeType;
    }

    public void setKnowledgeType(String knowledgeType) {
        this.knowledgeType = knowledgeType;
    }

    public List<Long> getTagIds() {
        return tagIds;
    }

    public void setTagIds(List<Long> tagIds) {
        this.tagIds = tagIds;
    }

    public LocalDateTime getScheduledPublishTime() {
        return scheduledPublishTime;
    }

    public void setScheduledPublishTime(LocalDateTime scheduledPublishTime) {
        this.scheduledPublishTime = scheduledPublishTime;
    }

    public String getChangeNote() {
        return changeNote;
    }

    public void setChangeNote(String changeNote) {
        this.changeNote = changeNote;
    }
}
