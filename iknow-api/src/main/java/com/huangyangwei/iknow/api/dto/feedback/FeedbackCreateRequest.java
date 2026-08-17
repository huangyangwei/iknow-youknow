package com.huangyangwei.iknow.api.dto.feedback;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 提交反馈请求：like/dislike/correction/suggestion。
 */
public class FeedbackCreateRequest {

    @NotBlank(message = "反馈类型不能为空")
    private String type;

    /** knowledge / answer */
    private String sourceType;

    private Long sourceId;

    /** 针对答案时关联会话 */
    private Long sessionId;

    private String question;

    @Size(max = 2000, message = "反馈内容过长")
    private String content;

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
}
