package com.huangyangwei.iknow.module.ai.config;

import com.huangyangwei.iknow.module.ai.retrieval.HybridCandidateRetriever;
import com.huangyangwei.iknow.module.ai.support.RetrievalCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Spring AI advisor retriever backed by the enhanced hybrid retrieval pipeline.
 */
public class HybridDocumentRetriever implements DocumentRetriever {

    private static final Logger log = LoggerFactory.getLogger(HybridDocumentRetriever.class);

    private final HybridCandidateRetriever candidateRetriever;
    private final DocumentRetriever fallback;

    public HybridDocumentRetriever(HybridCandidateRetriever candidateRetriever, DocumentRetriever fallback) {
        this.candidateRetriever = candidateRetriever;
        this.fallback = fallback;
    }

    @Override
    public List<Document> retrieve(Query query) {
        try {
            List<RetrievalCandidate> candidates = candidateRetriever.retrieve(query.text());
            if (candidates.isEmpty()) {
                return fallback.retrieve(query);
            }
            return candidates.stream().map(this::toDocument).toList();
        } catch (Exception e) {
            log.warn("hybrid document retrieval failed, falling back to default retriever: {}", e.getMessage());
            return fallback.retrieve(query);
        }
    }

    private Document toDocument(RetrievalCandidate candidate) {
        Map<String, Object> metadata = new LinkedHashMap<>(candidate.metadata());
        metadata.put("candidateId", candidate.candidateId());
        metadata.put("knowledgeId", candidate.knowledgeId());
        metadata.put("versionNo", candidate.versionNo());
        metadata.put("chunkIndex", candidate.chunkIndex());
        metadata.put("source", candidate.source());
        metadata.put("fusionScore", candidate.fusionScore());
        metadata.put("rerankScore", candidate.rerankScore());
        metadata.put("finalRank", candidate.finalRank());
        return Document.builder()
                .id(candidate.candidateId())
                .text(candidate.text())
                .metadata(metadata)
                .score(candidate.rerankScore() == null ? candidate.fusionScore() : candidate.rerankScore())
                .build();
    }
}
