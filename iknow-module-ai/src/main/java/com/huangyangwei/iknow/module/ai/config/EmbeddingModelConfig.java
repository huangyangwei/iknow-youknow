package com.huangyangwei.iknow.module.ai.config;

import com.huangyangwei.iknow.module.ai.support.DeterministicEmbeddingModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * EmbeddingModel 配置：支持硅基流动 BGE-M3（生产）和本地确定性模型（开发/测试）切换。
 * <p>
 * 通过 {@code spring.ai.model.embedding} 属性控制：
 * <ul>
 *   <li>{@code none}（默认）：使用本地确定性哈希模型（1024 维），无需外部服务，仅用于链路验证</li>
 *   <li>{@code openai}：显式构建指向硅基流动（OpenAI 兼容协议）的 BGE-M3（1024 维）embedding 模型。
 *       必须显式声明而不能依赖 OpenAI embedding auto-config：auto-config 绑定的顶层凭据
 *       {@code spring.ai.openai.api-key}（OPENAI_API_KEY）与硅基流动密钥（SILICONFLOW_API_KEY）
 *       隔离，且 {@code spring.ai.openai.embedding.base-url/api-key} 并非 auto-config 可靠识别的
 *       凭据绑定路径，依赖它会导致生产 embedding 凭据错配不可用</li>
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
        return new DeterministicEmbeddingModel();
    }

    /**
     * 生产环境：显式 SiliconFlow（OpenAI 兼容协议）embedding 模型（BAAI/bge-m3，1024 维）。
     * <p>
     * 声明为具体类型 {@link OpenAiEmbeddingModel}：{@code spring.ai.model.embedding=openai} 时
     * Spring AI 的 OpenAI embedding auto-config 也会激活，其 {@code @ConditionalOnMissingBean}
     * 按方法返回类型 {@code OpenAiEmbeddingModel} 回退，显式 bean 存在时 auto-config 不再重复创建，
     * 避免产生一个绑定错误凭据（OPENAI_API_KEY）的竞争 bean。
     * <p>
     * 凭据优先取 {@code spring.ai.openai.embedding.api-key}（application.yml 中映射
     * {@code SILICONFLOW_API_KEY}），兜底直接读环境变量；两者皆缺失时启动期快速失败并给出修复指引。
     * BGE-M3 原生 1024 维，硅基流动不支持 dimensions 参数，勿传。
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "spring.ai.model.embedding", havingValue = "openai")
    public OpenAiEmbeddingModel siliconFlowOpenAiEmbeddingModel(Environment environment) {
        String apiKey = environment.getProperty("spring.ai.openai.embedding.api-key");
        if (!StringUtils.hasText(apiKey)) {
            apiKey = environment.getProperty("SILICONFLOW_API_KEY");
        }
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("spring.ai.model.embedding=openai 但未配置硅基流动凭据："
                    + "请设置环境变量 SILICONFLOW_API_KEY（或配置项 spring.ai.openai.embedding.api-key）");
        }
        String baseUrl = environment.getProperty("spring.ai.openai.embedding.base-url",
                "https://api.siliconflow.cn/v1");
        String model = environment.getProperty("spring.ai.openai.embedding.model", "BAAI/bge-m3");
        return OpenAiEmbeddingModel.builder().options(OpenAiEmbeddingOptions.builder()
                        .model(model)
                        .apiKey(apiKey)
                        .baseUrl(baseUrl)
                .build()).build();
    }
}
