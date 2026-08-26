package com.huangyangwei.iknow.module.ai.config;

import com.huangyangwei.iknow.module.ai.support.DeterministicEmbeddingModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * EmbeddingModel 配置：支持硅基流动 BGE-M3（生产）和本地确定性模型（开发/测试）切换。
 * <p>
 * 通过 {@code spring.ai.model.embedding} 属性控制：
 * <ul>
 *   <li>{@code none}（默认）：使用本地确定性哈希模型（1024 维），无需外部服务，仅用于链路验证</li>
 *   <li>{@code openai}：由 Spring AI OpenAI Embedding auto-config 自动创建，指向硅基流动 BGE-M3（1024 维）</li>
 * </ul>
 * <p>
 * 切换方式：设置环境变量 {@code SPRING_AI_MODEL_EMBEDDING=openai}
 */
@Configuration
public class EmbeddingModelConfig {

    /**
     * 开发/测试环境：本地确定性模型（1024 维）。
     * 字符 unigram + bigram 哈希落到 1024 维桶并归一化，共享字符/词片段的文本获得较高余弦相似度。
     * {@code spring.ai.model.embedding=none} 或未设置时激活。
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "spring.ai.model.embedding", havingValue = "none", matchIfMissing = true)
    public EmbeddingModel deterministicEmbeddingModel() {
        return OpenAiEmbeddingModel.builder().options(OpenAiEmbeddingOptions.builder()
                        // BGE-M3 原生 1024 维，硅基流动不支持 dimensions 参数，勿传
                        .model("BAAI/bge-m3")
                        .apiKey(System.getenv("SILICONFLOW_API_KEY"))
                        .baseUrl("https://api.siliconflow.cn/v1")
                .build()).build();
    }
}
