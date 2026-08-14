package com.huangyangwei.iknow.module.ai.service;

import com.huangyangwei.iknow.common.constant.Constants;
import com.huangyangwei.iknow.module.ai.mapper.FtsHit;
import com.huangyangwei.iknow.module.ai.mapper.KbFtsMapper;
import com.huangyangwei.iknow.module.ai.support.Citation;
import com.huangyangwei.iknow.module.ai.support.RrfMerger;
import com.huangyangwei.iknow.module.knowledge.entity.KbKnowledge;
import com.huangyangwei.iknow.module.knowledge.mapper.KbKnowledgeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 混合检索（技术方案 §5.2）：pgvector HNSW 余弦召回 topK30 + PG 全文检索 topK20，
 * RRF 融合 → TopN(5~8)，过滤低分片段，强制 status='published'。
 * 无 pgvector 环境自动降级为纯 FTS。
 */
@Service
public class HybridRetrievalService {

    private static final Logger log = LoggerFactory.getLogger(HybridRetrievalService.class);

    private static final int VECTOR_TOP_K = 30;
    private static final int FTS_TOP_K = 20;
    private static final double VECTOR_SIMILARITY_THRESHOLD = 0.2;
    private static final int MAX_SNIPPET_LENGTH = 200;

    private final VectorStore vectorStore;
    private final KbFtsMapper ftsMapper;
    private final KbKnowledgeMapper knowledgeMapper;

    public HybridRetrievalService(VectorStore vectorStore, KbFtsMapper ftsMapper, KbKnowledgeMapper knowledgeMapper) {
        this.vectorStore = vectorStore;
        this.ftsMapper = ftsMapper;
        this.knowledgeMapper = knowledgeMapper;
    }

    /** RRF 融合后的引用列表（默认 TopN=6，限 5~8）。 */
    public List<Citation> retrieve(String question, int topN) {
        if (question == null || question.isBlank()) {
            return List.of();
        }
        int n = Math.max(5, Math.min(8, topN));

        List<VectorHit> vectorHits = vectorRetrieve(question);
        List<FtsHit> ftsHits = ftsRetrieve(question);

        List<Long> vectorIds = vectorHits.stream().map(VectorHit::knowledgeId).distinct().toList();
        List<Long> ftsIds = ftsHits.stream().map(FtsHit::getId).distinct().toList();

        List<RrfMerger.Ranked<Long>> merged = RrfMerger.merge(List.of(vectorIds, ftsIds));

        List<Citation> citations = new ArrayList<>();
        for (RrfMerger.Ranked<Long> ranked : merged) {
            if (citations.size() >= n) {
                break;
            }
            Citation citation = toCitation(ranked.item(), vectorHits, ftsHits);
            if (citation != null) {
                citations.add(citation);
            }
        }
        return citations;
    }

    private List<VectorHit> vectorRetrieve(String question) {
        try {
            List<Document> docs = vectorStore.similaritySearch(SearchRequest.builder()
                    .query(question)
                    .topK(VECTOR_TOP_K)
                    .similarityThreshold(VECTOR_SIMILARITY_THRESHOLD)
                    .build());
            return toPublishedVectorHits(docs);
        } catch (Exception e) {
            log.warn("vector retrieval unavailable, degrade to FTS only: {}", e.getMessage());
            return List.of();
        }
    }

    private List<VectorHit> toPublishedVectorHits(List<Document> docs) {
        if (docs.isEmpty()) {
            return List.of();
        }
        List<Long> knowledgeIds = docs.stream()
                .map(d -> asLong(d.getMetadata().get("knowledgeId")))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Set<Long> publishedIds = knowledgeIds.isEmpty() ? Set.of() : publishedIds(knowledgeIds);

        List<VectorHit> hits = new ArrayList<>();
        for (Document doc : docs) {
            Long knowledgeId = asLong(doc.getMetadata().get("knowledgeId"));
            if (knowledgeId == null || !publishedIds.contains(knowledgeId)) {
                continue;
            }
            hits.add(new VectorHit(knowledgeId,
                    asInt(doc.getMetadata().get("versionNo")),
                    asString(doc.getMetadata().get("title")),
                    doc.getText(),
                    doc.getScore()));
        }
        return hits;
    }

    private Set<Long> publishedIds(List<Long> ids) {
        return knowledgeMapper.selectBatchIds(ids).stream()
                .filter(k -> Constants.KNOWLEDGE_STATUS_PUBLISHED.equals(k.getStatus()))
                .map(KbKnowledge::getId)
                .collect(Collectors.toSet());
    }

    private List<FtsHit> ftsRetrieve(String question) {
        try {
            return ftsMapper.searchPublished(question, FTS_TOP_K);
        } catch (Exception e) {
            log.warn("fts retrieval failed: {}", e.getMessage());
            return List.of();
        }
    }

    private Citation toCitation(Long knowledgeId, List<VectorHit> vectorHits, List<FtsHit> ftsHits) {
        VectorHit best = null;
        for (VectorHit hit : vectorHits) {
            if (hit.knowledgeId().equals(knowledgeId) && (best == null || hit.score() > best.score())) {
                best = hit;
            }
        }
        if (best != null) {
            return new Citation(knowledgeId, best.versionNo(), best.title(), null,
                    snippet(best.chunkText()), best.score());
        }
        return ftsHits.stream()
                .filter(f -> f.getId().equals(knowledgeId))
                .findFirst()
                .map(f -> new Citation(knowledgeId, f.getVersionNo(), f.getTitle(), null,
                        snippet(f.getPlainText()), null))
                .orElse(null);
    }

    private String snippet(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= MAX_SNIPPET_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_SNIPPET_LENGTH) + "…";
    }

    private Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return value == null ? null : Long.valueOf(value.toString());
    }

    private Integer asInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return value == null ? null : Integer.valueOf(value.toString());
    }

    private String asString(Object value) {
        return value == null ? "" : value.toString();
    }

    private record VectorHit(Long knowledgeId, Integer versionNo, String title, String chunkText, Double score) {
    }
}
