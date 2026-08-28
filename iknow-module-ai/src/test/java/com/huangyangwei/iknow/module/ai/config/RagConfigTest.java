package com.huangyangwei.iknow.module.ai.config;

import com.huangyangwei.iknow.module.ai.rerank.HttpCrossEncoderReranker;
import com.huangyangwei.iknow.module.ai.rerank.NoopReranker;
import com.huangyangwei.iknow.module.ai.retrieval.HybridCandidateRetriever;
import org.junit.jupiter.api.Test;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

class RagConfigTest {

    private final RagConfig config = new RagConfig();

    @Test
    void documentRetrieverUsesHybridPipelineWhenEnhancedEnabled() {
        RagProperties properties = new RagProperties();

        DocumentRetriever retriever = config.documentRetriever(
                mock(VectorStore.class),
                properties,
                mock(HybridCandidateRetriever.class));

        assertInstanceOf(HybridDocumentRetriever.class, retriever);
    }

    @Test
    void documentRetrieverFallsBackToDefaultPipelineWhenEnhancedDisabled() {
        RagProperties properties = new RagProperties();
        properties.getEnhanced().setEnabled(false);

        DocumentRetriever retriever = config.documentRetriever(
                mock(VectorStore.class),
                properties,
                mock(HybridCandidateRetriever.class));

        assertInstanceOf(DegradableDocumentRetriever.class, retriever);
    }

    @Test
    void rerankerBeanDefaultsToNoopWithoutEndpoint() {
        assertInstanceOf(NoopReranker.class, config.reranker(new RagProperties(), new ObjectMapper()));
    }

    @Test
    void rerankerBeanUsesHttpCrossEncoderWhenEnabledAndEndpointConfigured() {
        RagProperties properties = new RagProperties();
        properties.getRerank().setEnabled(true);
        properties.getRerank().setEndpoint("http://localhost:8081/rerank");

        assertInstanceOf(HttpCrossEncoderReranker.class, config.reranker(properties, new ObjectMapper()));
    }
}
