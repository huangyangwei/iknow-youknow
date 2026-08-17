package com.huangyangwei.iknow.module.knowledge.event;

/**
 * 知识条目发布/回滚成功后发布（事务提交后由 AI 模块监听做向量化重建）。
 */
public record KnowledgePublishedEvent(Long knowledgeId, Integer versionNo, String title,
                                      String htmlContent, String plainText, Long userId) {
}
