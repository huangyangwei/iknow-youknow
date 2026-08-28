package com.huangyangwei.iknow.module.ai.retrieval;

import com.huangyangwei.iknow.module.ai.config.RagProperties;
import com.huangyangwei.iknow.module.ai.mapper.KbChunkFtsHit;
import com.huangyangwei.iknow.module.ai.mapper.KbChunkFtsMapper;
import com.huangyangwei.iknow.module.ai.mapper.KbFtsMapper;
import com.huangyangwei.iknow.module.ai.rerank.NoopReranker;
import com.huangyangwei.iknow.module.ai.rerank.Reranker;
import com.huangyangwei.iknow.module.ai.support.ChunkTextSplitter;
import com.huangyangwei.iknow.module.ai.support.ContextPruner;
import com.huangyangwei.iknow.module.ai.support.RetrievalCandidate;
import com.huangyangwei.iknow.module.knowledge.mapper.KbKnowledgeMapper;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HybridCandidateRetrieverTest {

    @Test
    void vectorFailureDegradesToKeywordOnlyChunkRetrieval() {
        VectorStore vectorStore = mock(VectorStore.class);
        KbChunkFtsMapper chunkFtsMapper = mock(KbChunkFtsMapper.class);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenThrow(new IllegalStateException("pgvector down"));
        when(chunkFtsMapper.searchPublishedChunks(eq("cache ttl"), anyInt())).thenReturn(List.of(
                hit(10L, 2, 3, "Cache", "cache ttl policy", 0.75)));

        HybridCandidateRetriever retriever = retriever(vectorStore, chunkFtsMapper, new NoopReranker());

        List<RetrievalCandidate> candidates = retriever.retrieve("cache ttl");

        assertEquals(1, candidates.size());
        RetrievalCandidate candidate = candidates.get(0);
        assertEquals("10:2:3", candidate.candidateId());
        assertEquals("keyword", candidate.source());
        assertEquals(3, candidate.chunkIndex());
        assertNotNull(candidate.fusionScore());
        assertEquals(1, candidate.finalRank());
    }

    @Test
    void rerankerCanReorderFusedCandidatesBeforePruningRanksThem() {
        VectorStore vectorStore = mock(VectorStore.class);
        KbChunkFtsMapper chunkFtsMapper = mock(KbChunkFtsMapper.class);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
        when(chunkFtsMapper.searchPublishedChunks(eq("ranking"), anyInt())).thenReturn(List.of(
                hit(1L, 1, 0, "First", "first chunk", 0.90),
                hit(2L, 1, 0, "Second", "second chunk", 0.80)));
        Reranker reranker = (query, candidates) -> List.of(
                candidates.get(1).withRerankScore(0.95),
                candidates.get(0).withRerankScore(0.10));

        HybridCandidateRetriever retriever = retriever(vectorStore, chunkFtsMapper, reranker);

        List<RetrievalCandidate> candidates = retriever.retrieve("ranking");

        assertEquals("2:1:0", candidates.get(0).candidateId());
        assertEquals(0.95, candidates.get(0).rerankScore());
        assertEquals(1, candidates.get(0).finalRank());
        assertEquals("1:1:0", candidates.get(1).candidateId());
        assertEquals(2, candidates.get(1).finalRank());
    }

    private HybridCandidateRetriever retriever(VectorStore vectorStore, KbChunkFtsMapper chunkFtsMapper,
                                               Reranker reranker) {
        RagProperties properties = new RagProperties();
        properties.getPrune().setMaxContextTokens(1000);
        return new HybridCandidateRetriever(
                vectorStore,
                chunkFtsMapper,
                mock(KbFtsMapper.class),
                mock(KbKnowledgeMapper.class),
                properties,
                reranker,
                new ContextPruner(properties, new ChunkTextSplitter()));
    }

    private KbChunkFtsHit hit(Long knowledgeId, Integer versionNo, Integer chunkIndex, String title, String content,
                              Double rank) {
        KbChunkFtsHit hit = new KbChunkFtsHit();
        hit.setKnowledgeId(knowledgeId);
        hit.setVersionNo(versionNo);
        hit.setChunkIndex(chunkIndex);
        hit.setTitle(title);
        hit.setContent(content);
        hit.setRank(rank);
        return hit;
    }
}
