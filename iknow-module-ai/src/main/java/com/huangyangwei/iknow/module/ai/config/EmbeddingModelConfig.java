package com.huangyangwei.iknow.module.ai.config;

import com.huangyangwei.iknow.module.ai.support.DeterministicEmbeddingModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Embedding model configuration.
 */
@Configuration
public class EmbeddingModelConfig {

    /**
     * Local deterministic embedding model for development and tests.
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "spring.ai.model.embedding", havingValue = "none", matchIfMissing = true)
    public EmbeddingModel deterministicEmbeddingModel() {
        return new DeterministicEmbeddingModel();
    }
}
