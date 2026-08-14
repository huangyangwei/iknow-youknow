package com.huangyangwei.iknow.spike2.config;

import com.huangyangwei.iknow.spike2.rag.ModelRouter;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 多模型注册：Spring AI 2.0 自动配置的 ChatClient.Builder 依赖唯一 ChatModel
 * （getIfUnique），多模型场景下必须为每个 ChatModel 手动构建 ChatClient。
 * 对应技术方案 ModelRouter（模型路由）。
 */
@Configuration
public class ModelConfig {

    @Bean
    public ChatClient claudeChatClient(@Qualifier("anthropicChatModel") ChatModel model) {
        return ChatClient.builder(model).build();
    }

    @Bean
    public ChatClient gptChatClient(@Qualifier("openAiChatModel") ChatModel model) {
        return ChatClient.builder(model).build();
    }

    @Bean
    public ChatClient geminiChatClient(@Qualifier("googleGenAiChatModel") ChatModel model) {
        return ChatClient.builder(model).build();
    }

    @Bean
    public ChatClient deepseekChatClient(@Qualifier("deepSeekChatModel") ChatModel model) {
        return ChatClient.builder(model).build();
    }

    @Bean
    public ModelRouter modelRouter(
            @Qualifier("claudeChatClient") ChatClient claudeChatClient,
            @Qualifier("gptChatClient") ChatClient gptChatClient,
            @Qualifier("geminiChatClient") ChatClient geminiChatClient,
            @Qualifier("deepseekChatClient") ChatClient deepseekChatClient) {
        Map<String, ChatClient> registry = new LinkedHashMap<>();
        registry.put("claude-opus-5", claudeChatClient);
        registry.put("gpt-4o", gptChatClient);
        registry.put("gemini-2.5-pro", geminiChatClient);
        registry.put("deepseek-v3", deepseekChatClient);
        return new ModelRouter(registry);
    }
}
