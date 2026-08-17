package com.huangyangwei.iknow.module.feedback.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.huangyangwei.iknow.common.entity.BaseEntity;

import java.time.LocalDateTime;

/**
 * 反馈记录：赞/踩/纠错/建议。status 流转 pending → processing → resolved。
 */
@TableName("fb_feedback")
public class FbFeedback extends BaseEntity {

    private String type;
    private String sourceType;
    private Long sourceId;
    private Long sessionId;
    private String question;
    private String content;
    private String status;
    private Long handlerId;
    private String handleNote;
    private LocalDateTime handledAt;
    private Long createdBy;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getHandlerId() {
        return handlerId;
    }

    public void setHandlerId(Long handlerId) {
        this.handlerId = handlerId;
    }

    public String getHandleNote() {
        return handleNote;
    }

    public void setHandleNote(String handleNote) {
        this.handleNote = handleNote;
    }

    public LocalDateTime getHandledAt() {
        return handledAt;
    }

    public void setHandledAt(LocalDateTime handledAt) {
        this.handledAt = handledAt;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }
}
