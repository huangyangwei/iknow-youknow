package com.huangyangwei.iknow.spike2;

import org.springframework.ai.model.chat.client.autoconfigure.ChatClientAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spike ② 入口：Spring AI 2.0 RAG 链路验证。
 * 多 ChatModel 场景下自动配置的 ChatClient.Builder 依赖唯一 ChatModel（getIfUnique），
 * 因此排除 ChatClientAutoConfiguration，由 ModelConfig 为每个模型手动构建 ChatClient。
 */
@SpringBootApplication(
        scanBasePackages = "com.huangyangwei.iknow.spike2",
        exclude = ChatClientAutoConfiguration.class)
public class Spike2RagApplication {

    public static void main(String[] args) {
        SpringApplication.run(Spike2RagApplication.class, args);
    }
}
