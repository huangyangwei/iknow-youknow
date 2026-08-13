package com.huangyangwei.iknow.spike2.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * RAG 服务：知识块写入（kb_chunk）、相似度检索、带引用上下文的流式/一次性问答。
 */
@Service
public class RagService {

    private static final String SYSTEM_PROMPT =
            "你是企业内部知识库问答助手。仅依据系统提供的参考资料回答，若资料不足请明确说明。";

    private final VectorStore vectorStore;
    private final RetrievalAugmentationAdvisor ragAdvisor;
    private final ModelRouter modelRouter;

    public RagService(VectorStore vectorStore, RetrievalAugmentationAdvisor ragAdvisor, ModelRouter modelRouter) {
        this.vectorStore = vectorStore;
        this.ragAdvisor = ragAdvisor;
        this.modelRouter = modelRouter;
    }

    /** 写入知识块到 kb_chunk */
    public void seed(List<Document> documents) {
        vectorStore.add(documents);
    }

    /** 相似度检索（不经过 LLM，返回命中文档供引用展示） */
    public List<Document> retrieve(String question, int topK) {
        return vectorStore.similaritySearch(SearchRequest.builder()
                .query(question)
                .topK(topK)
                .build());
    }

    /** RAG 流式问答（SSE） */
    public Flux<String> askStream(String modelKey, String question) {
        return modelRouter.get(modelKey)
                .prompt()
                .system(SYSTEM_PROMPT)
                .user(question)
                .advisors(ragAdvisor)
                .stream()
                .content();
    }

    /** RAG 一次性问答 */
    public String ask(String modelKey, String question) {
        return modelRouter.get(modelKey)
                .prompt()
                .system(SYSTEM_PROMPT)
                .user(question)
                .advisors(ragAdvisor)
                .call()
                .content();
    }
}
