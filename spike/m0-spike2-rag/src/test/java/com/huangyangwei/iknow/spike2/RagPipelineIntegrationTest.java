package com.huangyangwei.iknow.spike2;

import com.huangyangwei.iknow.spike2.rag.ModelRouter;
import com.huangyangwei.iknow.spike2.rag.RagService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spike ② 集成测试（依赖沙箱内 PG 16.14 + pgvector 0.8.6，端口 5433）：
 * 1. 四模型 ChatModel 注册（Anthropic/OpenAI/Gemini/DeepSeek）
 * 2. ModelRouter 模型路由
 * 3. PgVectorStore → 自建 kb_chunk 表：写入 + 相似度检索（HNSW 余弦）
 * 4. RetrievalAugmentationAdvisor（Modular RAG）链路装配
 */
@SpringBootTest
class RagPipelineIntegrationTest {

    @Autowired
    private List<ChatModel> chatModels;
    @Autowired
    private ModelRouter modelRouter;
    @Autowired
    private VectorStore vectorStore;
    @Autowired
    private RetrievalAugmentationAdvisor ragAdvisor;
    @Autowired
    private RagService ragService;

    @Test
    void multiModelChatModelsAreRegistered() {
        assertThat(chatModels).hasSize(4);
        assertThat(modelRouter.availableModels())
                .contains("claude-opus-5", "gpt-4o", "gemini-2.5-pro", "deepseek-v3");
    }

    @Test
    void ragAdvisorIsAssembled() {
        assertThat(ragAdvisor).isNotNull();
    }

    @Test
    void vectorStoreWritesAndRetrievesFromKbChunk() {
        Document d1 = Document.builder().id("itest-001")
                .text("pgvector 0.8 支持 HNSW 索引，余弦距离算子为 <=>，向量维度 1024。")
                .metadata(Map.of("knowledgeId", 2001L, "versionNo", 1, "chunkIndex", 0))
                .build();
        Document d2 = Document.builder().id("itest-002")
                .text("Spring AI 的 PgVectorStore 通过 vectorTableName 绑定自建表。")
                .metadata(Map.of("knowledgeId", 2002L, "versionNo", 1, "chunkIndex", 0))
                .build();

        vectorStore.add(List.of(d1, d2));
        try {
            List<Document> hits = vectorStore.similaritySearch(SearchRequest.builder()
                    .query("HNSW 索引与余弦距离")
                    .similarityThreshold(0.2)
                    .topK(3)
                    .build());
            assertThat(hits).isNotEmpty();
            assertThat(hits.get(0).getText()).contains("HNSW");
        } finally {
            vectorStore.delete(List.of("itest-001", "itest-002"));
        }
    }

    @Test
    void ragServiceRetrieveReturnsDocumentsWithoutLlm() {
        Document d = Document.builder().id("itest-003")
                .text("RetrievalAugmentationAdvisor 是 Modular RAG 的核心编排组件。")
                .metadata(Map.of("knowledgeId", 2003L, "versionNo", 1, "chunkIndex", 0))
                .build();
        vectorStore.add(List.of(d));
        try {
            List<Document> hits = ragService.retrieve("Modular RAG 核心组件", 2);
            assertThat(hits).extracting(Document::getText)
                    .anyMatch(t -> t.contains("RetrievalAugmentationAdvisor"));
        } finally {
            vectorStore.delete(List.of("itest-003"));
        }
    }
}
