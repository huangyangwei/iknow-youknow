package com.huangyangwei.iknow.module.ai.config;

import com.huangyangwei.iknow.module.ai.rerank.HttpCrossEncoderReranker;
import com.huangyangwei.iknow.module.ai.rerank.NoopReranker;
import com.huangyangwei.iknow.module.ai.rerank.Reranker;
import com.huangyangwei.iknow.module.ai.retrieval.HybridCandidateRetriever;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

/**
 * Modular RAG 组装（技术方案 §5.2）：DocumentRetriever（向量检索）+ QueryAugmenter
 * （上下文注入）组合为 RetrievalAugmentationAdvisor。PgVectorStore 由
 * spring-ai-starter-vector-store-pgvector 自动配置（指向自建 kb_chunk 表，
 * initialize-schema=false）。ChatMemory 提供多轮上下文窗口。
 */
@Configuration
@EnableConfigurationProperties(RagProperties.class)
public class RagConfig {

    @Bean
    public DocumentRetriever documentRetriever(VectorStore vectorStore, RagProperties properties,
                                               HybridCandidateRetriever hybridCandidateRetriever) {
        DocumentRetriever fallback = new DegradableDocumentRetriever(VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .similarityThreshold(0.2)
                .topK(8)
                .build());
        if (!properties.getEnhanced().isEnabled()) {
            return fallback;
        }
        return new HybridDocumentRetriever(hybridCandidateRetriever, fallback);
    }

    @Bean
    public Reranker reranker(RagProperties properties, ObjectMapper objectMapper) {
        RagProperties.Rerank rerank = properties.getRerank();
        if (rerank.isEnabled() && StringUtils.hasText(rerank.getEndpoint())) {
            return new HttpCrossEncoderReranker(rerank, objectMapper);
        }
        return new NoopReranker();
    }

    @Bean
    public RetrievalAugmentationAdvisor ragAdvisor(DocumentRetriever documentRetriever) {
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(documentRetriever)
                .queryAugmenter(ContextualQueryAugmenter.builder()
                        .allowEmptyContext(true)
                        .build())
                .build();
    }

    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .maxMessages(20)
                .build();
    }
}
