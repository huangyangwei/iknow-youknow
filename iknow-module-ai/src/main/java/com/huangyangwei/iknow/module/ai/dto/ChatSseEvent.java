package com.huangyangwei.iknow.module.ai.dto;

import com.huangyangwei.iknow.module.ai.support.Citation;
import com.huangyangwei.iknow.module.ai.support.ConfidenceEvaluator;

import java.util.List;

/**
 * 问答 SSE 事件（POST /api/chat/ask，text/event-stream）：
 * start（会话就绪）→ delta（流式增量）→ done（完整答案+置信度+引用）| error。
 */
public class ChatSseEvent {

    private String type;
    private String content;
    private String answer;
    private String model;
    private String modelName;
    private String confidence;
    private Double confidenceScore;
    private List<Citation> sources;
    private Long sessionId;
    private String message;

    public static ChatSseEvent start(Long sessionId) {
        ChatSseEvent event = new ChatSseEvent();
        event.type = "start";
        event.sessionId = sessionId;
        return event;
    }

    public static ChatSseEvent delta(String content) {
        ChatSseEvent event = new ChatSseEvent();
        event.type = "delta";
        event.content = content;
        return event;
    }

    public static ChatSseEvent done(String answer, String model, String modelName,
                                    ConfidenceEvaluator.Confidence confidence, List<Citation> sources,
                                    Long sessionId) {
        ChatSseEvent event = new ChatSseEvent();
        event.type = "done";
        event.answer = answer;
        event.model = model;
        event.modelName = modelName;
        event.confidence = confidence.level();
        event.confidenceScore = confidence.score();
        event.sources = sources;
        event.sessionId = sessionId;
        return event;
    }

    public static ChatSseEvent error(String message) {
        ChatSseEvent event = new ChatSseEvent();
        event.type = "error";
        event.message = message;
        return event;
    }

    public String getType() {
        return type;
    }

    public String getContent() {
        return content;
    }

    public String getAnswer() {
        return answer;
    }

    public String getModel() {
        return model;
    }

    public String getModelName() {
        return modelName;
    }

    public String getConfidence() {
        return confidence;
    }

    public Double getConfidenceScore() {
        return confidenceScore;
    }

    public List<Citation> getSources() {
        return sources;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public String getMessage() {
        return message;
    }
}
