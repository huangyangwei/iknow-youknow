package com.huangyangwei.iknow.module.ai.dto;

/**
 * 提问请求（POST /api/chat/ask）：sessionId 为空时新建会话。
 */
public record AskRequest(Long sessionId, String model, String question) {
}
