package com.huangyangwei.iknow.module.ai.support;

/**
 * 问答引用来源（技术方案 qa_message.sources JSONB：[{title,url,knowledgeId,versionNo,chunkText}]）。
 * score 为向量相似度（FTS 命中为 null）。
 */
public record Citation(Long knowledgeId, Integer versionNo, String title, String url,
                       String chunkText, Double score, Integer chunkIndex, String source,
                       Double fusionScore, Double rerankScore, Integer finalRank) {

    public Citation(Long knowledgeId, Integer versionNo, String title, String url,
                    String chunkText, Double score) {
        this(knowledgeId, versionNo, title, url, chunkText, score, null, null, null, null, null);
    }
}
