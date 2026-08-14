package com.huangyangwei.iknow.module.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.huangyangwei.iknow.common.entity.BaseEntity;

/**
 * 知识版本快照：每次发布/回滚生成一行，UNIQUE(knowledge_id, version_no)。
 */
@TableName("kb_knowledge_version")
public class KbKnowledgeVersion extends BaseEntity {

    private Long knowledgeId;
    private Integer versionNo;
    private String title;
    private String htmlContent;
    private String plainText;
    private String summary;
    private String changeNote;
    private Long createdBy;

    public Long getKnowledgeId() {
        return knowledgeId;
    }

    public void setKnowledgeId(Long knowledgeId) {
        this.knowledgeId = knowledgeId;
    }

    public Integer getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
    }

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

    public String getPlainText() {
        return plainText;
    }

    public void setPlainText(String plainText) {
        this.plainText = plainText;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getChangeNote() {
        return changeNote;
    }

    public void setChangeNote(String changeNote) {
        this.changeNote = changeNote;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }
}
