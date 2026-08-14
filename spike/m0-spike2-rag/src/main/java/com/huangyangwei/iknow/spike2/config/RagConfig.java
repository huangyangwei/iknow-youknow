package com.huangyangwei.iknow.spike2.config;

import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Modular RAG 组装：DocumentRetriever（向量检索）+ QueryAugmenter（上下文注入）
 * 组合为 RetrievalAugmentationAdvisor。PgVectorStore 由
 * spring-ai-starter-vector-store-pgvector 自动配置（指向自建 kb_chunk 表）。
 */
@Configuration
public class RagConfig {

    @Bean
    public VectorStoreDocumentRetriever documentRetriever(VectorStore vectorStore) {
        return VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .similarityThreshold(0.2)
                .topK(5)
                .build();
    }

    @Bean
    public RetrievalAugmentationAdvisor ragAdvisor(VectorStoreDocumentRetriever documentRetriever) {
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(documentRetriever)
                .queryAugmenter(ContextualQueryAugmenter.builder()
                        .allowEmptyContext(true)
                        .build())
                .build();
    }
}
