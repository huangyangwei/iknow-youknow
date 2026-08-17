package com.huangyangwei.iknow.module.ai.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;

import java.util.List;

/**
 * 向量检索降级包装：底层 VectorStore 不可用（如嵌入式 PG 无 pgvector、kb_chunk 未建表）
 * 时返回空文档而不是抛出异常，保证 RetrievalAugmentationAdvisor 以空上下文继续走
 * 问答链路（引用与置信度仍由 HybridRetrievalService 的 FTS 通道兜底）。生产环境
 * pgvector 可用时透明转发。
 */
public class DegradableDocumentRetriever implements DocumentRetriever {

    private static final Logger log = LoggerFactory.getLogger(DegradableDocumentRetriever.class);

    private final DocumentRetriever delegate;

    public DegradableDocumentRetriever(DocumentRetriever delegate) {
        this.delegate = delegate;
    }

    @Override
    public List<Document> retrieve(Query query) {
        try {
            return delegate.retrieve(query);
        } catch (Exception e) {
            log.debug("vector retrieval unavailable, degrade to empty context: {}", e.getMessage());
            return List.of();
        }
    }
}
