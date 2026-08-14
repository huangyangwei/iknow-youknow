package com.huangyangwei.iknow.module.ai.config;

import com.google.genai.Client;
import com.huangyangwei.iknow.module.ai.model.ChatModels;
import com.huangyangwei.iknow.module.ai.model.ModelInfo;
import com.huangyangwei.iknow.module.ai.model.ModelRouter;
import com.huangyangwei.iknow.module.ai.support.DeterministicChatModel;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 多模型注册（技术方案 §5.3）：Spring AI 2.0 自动配置的 ChatClient.Builder 依赖唯一
 * ChatModel（getIfUnique），多模型场景必须为每个 ChatModel 手动构建 ChatClient（M0 约束）。
 * <p>
 * 各 provider 的 ChatModel 是否生成取决于环境密钥（${ENV} 占位符，M0 约束：禁止明文凭据）。
 * 通过 ObjectProvider 按具体类型取 bean：密钥缺失时该 provider 不产生 ChatModel，
 * 对应 ChatClient 也就不注册，应用在无密钥的沙箱中仍可启动。
 * Google GenAI 的 auto-config 在无密钥时直接抛异常，故整体排除，改为按 GEMINI_API_KEY 手动构建。
 * deterministic 模型为本地桩，始终可用，用于沙箱演示完整 RAG 问答链路。
 */
@Configuration
public class ModelConfig {

    @Bean
    public ChatClient claudeChatClient(ObjectProvider<AnthropicChatModel> provider) {
        AnthropicChatModel model = provider.getIfAvailable();
        return model == null ? null : ChatClient.builder(model).build();
    }

    @Bean
    public ChatClient gptChatClient(ObjectProvider<OpenAiChatModel> provider) {
        OpenAiChatModel model = provider.getIfAvailable();
        return model == null ? null : ChatClient.builder(model).build();
    }

    @Bean
    public ChatClient geminiChatClient(@Value("${GEMINI_API_KEY:}") String apiKey,
                                       ObjectProvider<ToolCallingManager> toolCallingManager) {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        Client genAiClient = Client.builder().apiKey(apiKey).build();
        ToolCallingManager manager = toolCallingManager.getIfAvailable();
        if (manager == null) {
            manager = DefaultToolCallingManager.builder().build();
        }
        GoogleGenAiChatModel model = GoogleGenAiChatModel.builder()
                .genAiClient(genAiClient)
                .toolCallingManager(manager)
                .build();
        return ChatClient.builder(model).build();
    }

    @Bean
    public ChatClient deepseekChatClient(@Value("${DEEPSEEK_API_KEY:}") String apiKey,
                                         @Value("${DEEPSEEK_BASE_URL:https://api.deepseek.com}") String baseUrl) {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        DeepSeekApi deepSeekApi = DeepSeekApi.builder().baseUrl(baseUrl).apiKey(apiKey).build();
        DeepSeekChatModel model = DeepSeekChatModel.builder().deepSeekApi(deepSeekApi).build();
        return ChatClient.builder(model).build();
    }

    @Bean
    public ChatClient deterministicChatClient() {
        return ChatClient.builder(new DeterministicChatModel()).build();
    }

    @Bean
    public ModelRouter modelRouter(Map<String, ChatClient> chatClients) {
        Map<String, String> keyByBean = new LinkedHashMap<>();
        keyByBean.put("claudeChatClient", ChatModels.CLAUDE_OPUS_5);
        keyByBean.put("gptChatClient", ChatModels.GPT_4O);
        keyByBean.put("geminiChatClient", ChatModels.GEMINI_2_5_PRO);
        keyByBean.put("deepseekChatClient", ChatModels.DEEPSEEK_V3);

        Map<String, ChatClient> registry = new LinkedHashMap<>();
        for (Map.Entry<String, ChatClient> entry : chatClients.entrySet()) {
            String key = keyByBean.get(entry.getKey());
            if (key != null) {
                registry.put(key, entry.getValue());
            }
        }
        // 本地确定性桩始终可用（沙箱无密钥时唯一可调用的模型，真实环境作为补充）。
        ChatClient deterministic = chatClients.get("deterministicChatClient");
        if (deterministic != null) {
            registry.put(ChatModels.DETERMINISTIC, deterministic);
        }

        // /api/models 只暴露实际注册了 ChatClient 的模型（密钥缺失的 provider 不列出）。
        Map<String, ModelInfo> infoByKey = new LinkedHashMap<>();
        infoByKey.put(ChatModels.CLAUDE_OPUS_5, new ModelInfo(ChatModels.CLAUDE_OPUS_5, "Claude Opus 5", "Anthropic 旗舰模型（OpenAI 兼容协议）"));
        infoByKey.put(ChatModels.GPT_4O, new ModelInfo(ChatModels.GPT_4O, "GPT-4o", "OpenAI 多模态旗舰"));
        infoByKey.put(ChatModels.GEMINI_2_5_PRO, new ModelInfo(ChatModels.GEMINI_2_5_PRO, "Gemini 2.5 Pro", "Google 旗舰长上下文模型"));
        infoByKey.put(ChatModels.DEEPSEEK_V3, new ModelInfo(ChatModels.DEEPSEEK_V3, "DeepSeek V3", "高性价比开源模型"));
        infoByKey.put(ChatModels.DETERMINISTIC, new ModelInfo(ChatModels.DETERMINISTIC, "本地确定性模型", "沙箱演示桩：无外部 API，返回固定模板回答（验证链路用）"));

        List<ModelInfo> models = infoByKey.values().stream()
                .filter(info -> registry.containsKey(info.key()))
                .toList();
        return new ModelRouter(registry, models);
    }
}
