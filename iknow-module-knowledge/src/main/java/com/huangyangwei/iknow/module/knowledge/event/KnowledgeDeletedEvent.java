package com.huangyangwei.iknow.module.knowledge.event;

/**
 * 知识条目删除成功后发布（事务提交后由 AI 模块监听清理对应向量块）。
 */
public record KnowledgeDeletedEvent(Long knowledgeId) {
}
