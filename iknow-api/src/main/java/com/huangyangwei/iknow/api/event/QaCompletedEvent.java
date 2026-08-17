package com.huangyangwei.iknow.api.event;

/**
 * 问答完成事件（P3 统计落库契约）：AI 模块流式回答完成后发布，
 * analytics 模块监听并写入 stat_query_log（query_type=qa）。
 *
 * @param question 用户提问（作为 keyword）
 * @param hasResult 是否检索到知识库引用
 * @param userId 提问用户
 */
public record QaCompletedEvent(String question, boolean hasResult, Long userId) {
}
