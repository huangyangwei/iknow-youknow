package com.huangyangwei.iknow.module.ai.rerank;

import com.huangyangwei.iknow.module.ai.support.RetrievalCandidate;

import java.util.List;

/**
 * Default reranker that preserves weighted RRF ordering.
 */
public class NoopReranker implements Reranker {

    @Override
    public List<RetrievalCandidate> rerank(String query, List<RetrievalCandidate> candidates) {
        return candidates == null ? List.of() : candidates;
    }
}
