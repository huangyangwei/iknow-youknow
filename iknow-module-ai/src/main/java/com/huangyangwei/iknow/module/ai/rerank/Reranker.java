package com.huangyangwei.iknow.module.ai.rerank;

import com.huangyangwei.iknow.module.ai.support.RetrievalCandidate;

import java.util.List;

/**
 * Optional post-fusion reranker for retrieved chunks.
 */
public interface Reranker {

    List<RetrievalCandidate> rerank(String query, List<RetrievalCandidate> candidates);
}
