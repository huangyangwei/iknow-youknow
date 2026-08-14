package com.huangyangwei.iknow.spike2.rag;

import org.springframework.ai.chat.client.ChatClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 模型路由：按业务 key（claude-opus-5 / gpt-4o / gemini-2.5-pro / deepseek-v3）选择 ChatClient。
 */
public class ModelRouter {

    private final Map<String, ChatClient> clients;

    public ModelRouter(Map<String, ChatClient> clients) {
        this.clients = new LinkedHashMap<>(clients);
    }

    public List<String> availableModels() {
        return new ArrayList<>(clients.keySet());
    }

    public ChatClient get(String model) {
        ChatClient client = clients.get(model);
        if (client == null) {
            throw new IllegalArgumentException("未知模型: " + model + "，可用: " + availableModels());
        }
        return client;
    }

    public ChatClient defaultClient() {
        return clients.values().iterator().next();
    }
}
