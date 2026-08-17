package com.huangyangwei.iknow.module.ai.service;

import com.huangyangwei.iknow.module.ai.support.ChunkTextSplitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 发布时向量化（技术方案 §5.1）：纯文本 → 512 token 重叠分块 → 1024 维 embedding → kb_chunk。
 * 块 id 采用确定性 {knowledgeId}-{versionNo}-{chunkIndex}，重复发布幂等覆盖。
 * 由 {@code KnowledgePublishedEvent}/{@code KnowledgeDeletedEvent} 事务提交后触发。
 * 无 pgvector 环境（嵌入式 PG）优雅降级：跳过并告警，不阻断发布业务。
 */
@Service
public class ChunkVectorizationService {

    private static final Logger log = LoggerFactory.getLogger(ChunkVectorizationService.class);

    private final VectorStore vectorStore;
    private final ChunkTextSplitter splitter;
    private final JdbcTemplate jdbcTemplate;

    public ChunkVectorizationService(VectorStore vectorStore, ChunkTextSplitter splitter, JdbcTemplate jdbcTemplate) {
        this.vectorStore = vectorStore;
        this.splitter = splitter;
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 重建某知识的向量块：删除旧块后按当前版本重新切分+嵌入。 */
    public void reindexKnowledge(Long knowledgeId, Integer versionNo, String title, String plainText) {
        try {
            deleteByKnowledgeId(knowledgeId);
            List<String> chunks = splitter.split(plainText);
            if (chunks.isEmpty()) {
                return;
            }
            List<Document> docs = new ArrayList<>(chunks.size());
            for (int i = 0; i < chunks.size(); i++) {
                Map<String, Object> metadata = new LinkedHashMap<>();
                metadata.put("knowledgeId", knowledgeId);
                metadata.put("versionNo", versionNo);
                metadata.put("chunkIndex", i);
                metadata.put("title", title == null ? "" : title);
                docs.add(Document.builder()
                        .id(knowledgeId + "-" + versionNo + "-" + i)
                        .text(chunks.get(i))
                        .metadata(metadata)
                        .build());
            }
            vectorStore.add(docs);
            log.info("vectorized knowledge {} version {} -> {} chunks", knowledgeId, versionNo, chunks.size());
        } catch (Exception e) {
            log.warn("vectorize knowledge {} version {} skipped: {}", knowledgeId, versionNo, e.getMessage());
        }
    }

    /** 按 knowledge_id 删除全部向量块（metadata.knowledgeId）。 */
    public void deleteByKnowledgeId(Long knowledgeId) {
        try {
            jdbcTemplate.update("DELETE FROM kb_chunk WHERE (metadata->>'knowledgeId')::bigint = ?", knowledgeId);
            log.info("deleted vector chunks for knowledge {}", knowledgeId);
        } catch (Exception e) {
            log.warn("delete vector chunks for knowledge {} skipped: {}", knowledgeId, e.getMessage());
        }
    }
}
