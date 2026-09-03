package com.huangyangwei.iknow.module.ai.config;

import com.huangyangwei.iknow.module.ai.support.DeterministicEmbeddingModel;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HYW-31 回归守卫（openai 模式启动装配）：
 * <ul>
 *   <li>none（默认）模式只装配本地确定性模型，OpenAI embedding auto-config 不介入；</li>
 *   <li>openai 模式显式装配硅基础流动 BGE-M3（OpenAI 兼容协议），凭据/地址取自
 *       spring.ai.openai.embedding.*，OpenAI embedding auto-config（顶层 OPENAI_API_KEY 凭据）
 *       必须因 @ConditionalOnMissingBean 回退，不得产生凭据错配的竞争 bean；</li>
 *   <li>openai 模式缺凭据时启动期快速失败并给出修复指引。</li>
 * </ul>
 * 发布向量化链路（ChunkVectorizationService → VectorStore → EmbeddingModel）按类型注入
 * EmbeddingModel，上述装配正确性直接决定发布向量化的实际模型指向。
 */
class EmbeddingModelConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(EmbeddingModelConfig.class)
            .withConfiguration(AutoConfigurations.of(OpenAiEmbeddingAutoConfiguration.class));

    @Test
    void noneModeWiresDeterministicModelOnly() {
        runner.withPropertyValues("spring.ai.model.embedding=none")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).getBeanNames(EmbeddingModel.class)
                            .containsExactly("deterministicEmbeddingModel");
                    assertThat(context.getBean(EmbeddingModel.class))
                            .isInstanceOf(DeterministicEmbeddingModel.class);
                });
    }

    @Test
    void openaiModeWiresExplicitSiliconFlowModelAndBacksOffAutoConfiguration() {
        runner.withPropertyValues(
                        "spring.ai.model.embedding=openai",
                        "spring.ai.openai.embedding.api-key=test-siliconflow-key",
                        "spring.ai.openai.embedding.base-url=https://api.siliconflow.cn/v1",
                        "spring.ai.openai.embedding.model=BAAI/bge-m3")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    // 显式 bean 唯一存在：auto-config（顶层 OPENAI_API_KEY 凭据）必须回退
                    assertThat(context).getBeanNames(EmbeddingModel.class)
                            .containsExactly("siliconFlowOpenAiEmbeddingModel");
                    OpenAiEmbeddingModel model = context.getBean(OpenAiEmbeddingModel.class);
                    assertThat(model.getOptions().getModel()).isEqualTo("BAAI/bge-m3");
                    assertThat(model.getOptions().getBaseUrl()).isEqualTo("https://api.siliconflow.cn/v1");
                    assertThat(model.getOptions().getApiKey()).isEqualTo("test-siliconflow-key");
                });
    }

    @Test
    void openaiModeFallsBackToEnvironmentVariableCredentials() {
        // spring.ai.openai.embedding.api-key 未配置时，兜底读环境变量 SILICONFLOW_API_KEY
        runner.withPropertyValues(
                        "spring.ai.model.embedding=openai",
                        "SILICONFLOW_API_KEY=env-siliconflow-key")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    OpenAiEmbeddingModel model = context.getBean(OpenAiEmbeddingModel.class);
                    assertThat(model.getOptions().getApiKey()).isEqualTo("env-siliconflow-key");
                    assertThat(model.getOptions().getBaseUrl()).isEqualTo("https://api.siliconflow.cn/v1");
                    assertThat(model.getOptions().getModel()).isEqualTo("BAAI/bge-m3");
                });
    }

    @Test
    void openaiModeWithoutCredentialsFailsFastWithGuidance() {
        runner.withPropertyValues("spring.ai.model.embedding=openai")
                .run(context -> {
                    assertThat(context).hasFailed();
                    Throwable failure = context.getStartupFailure();
                    assertThat(failure).isInstanceOf(BeanCreationException.class);
                    assertThat(failure).hasRootCauseInstanceOf(IllegalStateException.class);
                    assertThat(failure).hasStackTraceContaining("SILICONFLOW_API_KEY");
                });
    }
}
