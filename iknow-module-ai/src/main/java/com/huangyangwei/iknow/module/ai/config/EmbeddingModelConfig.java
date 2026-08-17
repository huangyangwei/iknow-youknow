package com.huangyangwei.iknow.module.ai.config;

import com.huangyangwei.iknow.module.ai.support.DeterministicEmbeddingModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * EmbeddingModel：本地确定性模型（1024 维）。@Primary 确保 PgVectorStore
 * 注入唯一 EmbeddingModel（各模型 starter 的 embedding auto-config 默认关闭）。
 * 生产环境替换为 BGE-M3 等真实 embedding 模型（M0 结论）。
 */
@Configuration
public class EmbeddingModelConfig {

    @Bean
    @Primary
    public EmbeddingModel embeddingModel() {
        return new DeterministicEmbeddingModel();
    }
}
