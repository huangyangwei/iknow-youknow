package com.huangyangwei.iknow.module.ai.dto;

import com.huangyangwei.iknow.module.ai.entity.QaSession;

import java.time.LocalDateTime;

/**
 * 聊天会话响应。雪花 ID 以字符串返回，避免前端 JS Number 精度丢失。
 */
public record ChatSessionResponse(String id,
                                  String title,
                                  LocalDateTime createdAt,
                                  LocalDateTime updatedAt) {

    public static ChatSessionResponse from(QaSession session) {
        return new ChatSessionResponse(
                stringify(session.getId()),
                session.getTitle(),
                session.getCreatedAt(),
                session.getUpdatedAt());
    }

    private static String stringify(Long value) {
        return value == null ? null : String.valueOf(value);
    }
}
