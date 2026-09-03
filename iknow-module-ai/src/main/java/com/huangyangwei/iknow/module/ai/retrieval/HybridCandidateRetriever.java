package com.huangyangwei.iknow.module.ai.retrieval;

import com.huangyangwei.iknow.common.constant.Constants;
import com.huangyangwei.iknow.module.ai.config.RagProperties;
import com.huangyangwei.iknow.module.ai.mapper.FtsHit;
import com.huangyangwei.iknow.module.ai.mapper.KbChunkFtsHit;
import com.huangyangwei.iknow.module.ai.mapper.KbChunkFtsMapper;
import com.huangyangwei.iknow.module.ai.mapper.KbFtsMapper;
import com.huangyangwei.iknow.module.ai.rerank.Reranker;
import com.huangyangwei.iknow.module.ai.support.ContextPruner;
import com.huangyangwei.iknow.module.ai.support.HybridCandidateFusion;
import com.huangyangwei.iknow.module.ai.support.RetrievalCandidate;
import com.huangyangwei.iknow.module.knowledge.entity.KbKnowledge;
import com.huangyangwei.iknow.module.knowledge.mapper.KbKnowledgeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Enhanced hybrid chunk retrieval: vector recall + chunk FTS + weighted RRF + optional rerank + pruning.
 */
@Service
public class HybridCandidateRetriever {

    private static final Logger log = LoggerFactory.getLogger(HybridCandidateRetriever.class);

    private static final double VECTOR_SIMILARITY_THRESHOLD = 0.2;

    private final VectorStore vectorStore;
    private final KbChunkFtsMapper chunkFtsMapper;
    private final KbFtsMapper legacyFtsMapper;
    private final KbKnowledgeMapper knowledgeMapper;
    private final RagProperties properties;
    private final Reranker reranker;
    private final ContextPruner contextPruner;

    public HybridCandidateRetriever(VectorStore vectorStore, KbChunkFtsMapper chunkFtsMapper,
                                    KbFtsMapper legacyFtsMapper, KbKnowledgeMapper knowledgeMapper,
                                    RagProperties properties, Reranker reranker, ContextPruner contextPruner) {
        this.vectorStore = vectorStore;
        this.chunkFtsMapper = chunkFtsMapper;
        this.legacyFtsMapper = legacyFtsMapper;
        this.knowledgeMapper = knowledgeMapper;
        this.properties = properties;
        this.reranker = reranker;
        this.contextPruner = contextPruner;
    }

    public List<RetrievalCandidate> retrieve(String question) {
        if (!StringUtils.hasText(question)) {
            return List.of();
        }

        List<RetrievalCandidate> vectorCandidates = vectorRetrieve(question);
        List<RetrievalCandidate> keywordCandidates = chunkFtsRetrieve(question);
        List<RetrievalCandidate> fused = HybridCandidateFusion.fuse(vectorCandidates, keywordCandidates,
                properties.getFusion().getVectorWeight(), properties.getFusion().getKeywordWeight(),
                properties.getFusion().getRrfK());

        if (fused.isEmpty()) {
            fused = legacyKeywordFallback(question);
        }
        if (fused.isEmpty()) {
            return List.of();
        }

        List<RetrievalCandidate> reranked = rerank(question, fused);
        return contextPruner.prune(reranked);
    }

    private List<RetrievalCandidate> vectorRetrieve(String question) {
        try {
            List<Document> docs = vectorStore.similaritySearch(SearchRequest.builder()
                    .query(question)
                    .topK(Math.max(1, properties.getVector().getTopK()))
                    .similarityThreshold(VECTOR_SIMILARITY_THRESHOLD)
                    .build());
            return toPublishedVectorCandidates(docs);
        } catch (Exception e) {
            log.warn("vector retrieval unavailable, degrade to keyword channel: {}", e.getMessage());
            return List.of();
        }
    }

    private List<RetrievalCandidate> toPublishedVectorCandidates(List<Document> docs) {
        if (docs == null || docs.isEmpty()) {
            return List.of();
        }
        List<Long> knowledgeIds = docs.stream()
                .map(document -> asLong(document.getMetadata().get("knowledgeId")))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Set<Long> publishedIds = knowledgeIds.isEmpty() ? Set.of() : publishedIds(knowledgeIds);

        List<RetrievalCandidate> candidates = new ArrayList<>();
        for (Document doc : docs) {
            Long knowledgeId = asLong(doc.getMetadata().get("knowledgeId"));
            Integer versionNo = asInt(doc.getMetadata().get("versionNo"));
            Integer chunkIndex = asInt(doc.getMetadata().get("chunkIndex"));
            if (knowledgeId == null || versionNo == null || chunkIndex == null || !publishedIds.contains(knowledgeId)) {
                continue;
            }
            Map<String, Object> metadata = new LinkedHashMap<>(doc.getMetadata());
            metadata.put("candidateId", candidateId(knowledgeId, versionNo, chunkIndex));
            candidates.add(new RetrievalCandidate(candidateId(knowledgeId, versionNo, chunkIndex),
                    knowledgeId, versionNo, chunkIndex, asString(metadata.get("title")), asString(metadata.get("url")),
                    doc.getText(), metadata, "vector", doc.getScore(), null, null, null, null));
        }
        return candidates;
    }

    private Set<Long> publishedIds(List<Long> ids) {
        return knowledgeMapper.selectBatchIds(ids).stream()
                .filter(knowledge -> Constants.KNOWLEDGE_STATUS_PUBLISHED.equals(knowledge.getStatus()))
                .map(KbKnowledge::getId)
                .collect(Collectors.toSet());
    }

    private List<RetrievalCandidate> chunkFtsRetrieve(String question) {
        try {
            return chunkFtsMapper.searchPublishedChunks(question, Math.max(1, properties.getKeyword().getTopK()))
                    .stream()
                    .map(this::toKeywordCandidate)
                    .toList();
        } catch (Exception e) {
            log.warn("chunk fts retrieval unavailable, attempting legacy knowledge FTS fallback: {}", e.getMessage());
            return List.of();
        }
    }

    private RetrievalCandidate toKeywordCandidate(KbChunkFtsHit hit) {
        Integer chunkIndex = hit.getChunkIndex() == null ? 0 : hit.getChunkIndex();
        Long knowledgeId = hit.getKnowledgeId();
        Integer versionNo = hit.getVersionNo();
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("knowledgeId", knowledgeId);
        metadata.put("versionNo", versionNo);
        metadata.put("chunkIndex", chunkIndex);
        metadata.put("title", hit.getTitle());
        String candidateId = candidateId(knowledgeId, versionNo, chunkIndex);
        metadata.put("candidateId", candidateId);
        return new RetrievalCandidate(candidateId, knowledgeId, versionNo, chunkIndex, hit.getTitle(), null,
                hit.getContent(), metadata, "keyword", null, hit.getRank(), null, null, null);
    }

    private List<RetrievalCandidate> legacyKeywordFallback(String question) {
        try {
            List<FtsHit> hits = legacyFtsMapper.searchPublished(question, Math.max(1, properties.getKeyword().getTopK()));
            List<RetrievalCandidate> candidates = new ArrayList<>(hits.size());
            for (FtsHit hit : hits) {
                int chunkIndex = 0;
                String candidateId = candidateId(hit.getId(), hit.getVersionNo(), chunkIndex);
                Map<String, Object> metadata = new LinkedHashMap<>();
                metadata.put("knowledgeId", hit.getId());
                metadata.put("versionNo", hit.getVersionNo());
                metadata.put("chunkIndex", chunkIndex);
                metadata.put("title", hit.getTitle());
                metadata.put("candidateId", candidateId);
                candidates.add(new RetrievalCandidate(candidateId, hit.getId(), hit.getVersionNo(), chunkIndex,
                        hit.getTitle(), null, hit.getPlainText(), metadata, "keyword", null, hit.getRank(),
                        null, null, null));
            }
            return candidates;
        } catch (Exception e) {
            log.warn("legacy knowledge FTS fallback failed: {}", e.getMessage());
            return List.of();
        }
    }

    private List<RetrievalCandidate> rerank(String question, List<RetrievalCandidate> candidates) {
        try {
            return reranker.rerank(question, candidates);
        } catch (Exception e) {
            log.warn("reranker failed, falling back to fusion order: {}", e.getMessage());
            return candidates;
        }
    }

    public static String candidateId(Long knowledgeId, Integer versionNo, Integer chunkIndex) {
        return knowledgeId + ":" + versionNo + ":" + chunkIndex;
    }

    private Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return value == null || !StringUtils.hasText(value.toString()) ? null : Long.valueOf(value.toString());
    }

    private Integer asInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return value == null || !StringUtils.hasText(value.toString()) ? null : Integer.valueOf(value.toString());
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }
}
