package com.huangyangwei.iknow.spike2.demo;

import com.huangyangwei.iknow.spike2.rag.ModelRouter;
import com.huangyangwei.iknow.spike2.rag.RagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 端到端 Demo：写入 kb_chunk → 相似度检索 → 各模型 RAG 流式问答（SSE 分片）。
 * 通过 --app.run-demo=true 或环境变量 RUN_DEMO=true 开启。
 * 验证模型依次为 deepseek-v3（OpenAI 兼容协议）、claude-opus-5（Anthropic 协议）。
 */
@Component
public class RagDemoRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(RagDemoRunner.class);

    private static final String QUESTION = "kb_chunk 表的 HNSW 索引采用哪种距离算子？向量维度锁定多少？";

    private final RagService ragService;
    private final ModelRouter modelRouter;

    @Value("${app.run-demo:false}")
    private boolean runDemo;

    public RagDemoRunner(RagService ragService, ModelRouter modelRouter) {
        this.ragService = ragService;
        this.modelRouter = modelRouter;
    }

    @Override
    public void run(String... args) {
        if (!runDemo) {
            return;
        }
        log.info("=== RAG Demo 开始，可用模型: {}", modelRouter.availableModels());
        seedSampleChunks();

        log.info("--- 1. 相似度检索验证 ---");
        ragService.retrieve(QUESTION, 3)
                .forEach(d -> log.info("命中: {}", d.getText()));

        log.info("--- 2. 各模型 RAG 流式问答验证 ---");
        for (String model : List.of("deepseek-v3", "claude-opus-5", "gpt-4o")) {
            verifyStreaming(model);
        }

        log.info("--- 3. 非流式问答（deepseek-v3） ---");
        java.util.concurrent.ExecutorService exec = java.util.concurrent.Executors.newSingleThreadExecutor();
        java.util.concurrent.FutureTask<String> syncTask = new java.util.concurrent.FutureTask<>(
                () -> ragService.ask("deepseek-v3", QUESTION));
        exec.execute(syncTask);
        try {
            String answer = syncTask.get(60, java.util.concurrent.TimeUnit.SECONDS);
            log.info("同步回答: {}", answer);
        } catch (Exception e) {
            log.error("同步问答失败/超时: {}", e.getMessage());
        } finally {
            exec.shutdownNow();
        }

        log.info("=== RAG Demo 结束 ===");
        // OkHttp 等非守护线程会阻止 JVM 退出，Demo 模式显式退出
        System.exit(0);
    }

    private void verifyStreaming(String model) {
        log.info("--- [{}] 流式问答 ---", model);
        try {
            StringBuilder sb = new StringBuilder();
            Flux<String> stream = ragService.askStream(model, QUESTION);
            stream.doOnNext(sb::append)
                    .doOnComplete(() -> log.info("[{}] 流式完成，收到 {} 个分片，答案: {}", model, countChunks(sb), sb))
                    .blockLast(Duration.ofSeconds(120));
        } catch (Exception e) {
            log.error("[{}] 流式问答失败（网络/key 不可用属预期）: {}", model, e.getMessage());
        }
    }

    private int countChunks(StringBuilder sb) {
        return sb.length();
    }

    private void seedSampleChunks() {
        List<Document> docs = List.of(
                Document.builder().id("chunk-001")
                        .text("kb_chunk 表使用 pgvector 0.8，向量维度锁定 1024，采用 HNSW 索引与余弦距离（<=>）。")
                        .metadata(Map.of("knowledgeId", 1001L, "versionNo", 1, "chunkIndex", 0))
                        .build(),
                Document.builder().id("chunk-002")
                        .text("Spring AI 2.0 中 PgVectorStore 通过 vectorTableName 指向自建 kb_chunk 表，并指定 dimensions 与 distanceType。")
                        .metadata(Map.of("knowledgeId", 1001L, "versionNo", 1, "chunkIndex", 1))
                        .build(),
                Document.builder().id("chunk-003")
                        .text("RetrievalAugmentationAdvisor 组合 DocumentRetriever 与 ContextualQueryAugmenter，实现 Modular RAG 链路。")
                        .metadata(Map.of("knowledgeId", 1002L, "versionNo", 1, "chunkIndex", 0))
                        .build());
        ragService.seed(docs);
        log.info("已写入 {} 个知识块到 kb_chunk", docs.size());
    }
}
