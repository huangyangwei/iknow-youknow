package com.huangyangwei.iknow.module.ai.dto;

import com.huangyangwei.iknow.module.ai.entity.QaMessage;
import com.huangyangwei.iknow.module.ai.support.Citation;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 聊天历史消息响应。雪花 ID 以字符串返回，避免前端 JS Number 精度丢失。
 */
public record ChatMessageResponse(String id,
                                  String sessionId,
                                  String role,
                                  String content,
                                  String model,
                                  String confidence,
                                  List<Citation> sources,
                                  Integer tokens,
                                  LocalDateTime createdAt,
                                  LocalDateTime updatedAt) {

    public static ChatMessageResponse from(QaMessage message, List<Citation> sources) {
        return new ChatMessageResponse(
                stringify(message.getId()),
                stringify(message.getSessionId()),
                message.getRole(),
                message.getContent(),
                message.getModel(),
                message.getConfidence(),
                sources,
                message.getTokens(),
                message.getCreatedAt(),
                message.getUpdatedAt());
    }

    private static String stringify(Long value) {
        return value == null ? null : String.valueOf(value);
    }
}
