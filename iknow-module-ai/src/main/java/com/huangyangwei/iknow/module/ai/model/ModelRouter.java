package com.huangyangwei.iknow.module.ai.model;

import org.springframework.ai.chat.client.ChatClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 模型路由（技术方案 §5.3 ModelRouter）：按业务 key 选择 ChatClient；
 * 注册表由 ModelConfig 以 LinkedHashMap 注入，保持可预测的默认顺序。
 */
public class ModelRouter {

    private final Map<String, ChatClient> clients;
    private final List<ModelInfo> models;

    public ModelRouter(Map<String, ChatClient> clients, List<ModelInfo> models) {
        this.clients = new LinkedHashMap<>(clients);
        this.models = List.copyOf(models);
    }

    public List<ModelInfo> availableModels() {
        return models;
    }

    public String displayName(String key) {
        return models.stream()
                .filter(m -> m.key().equals(key))
                .map(ModelInfo::name)
                .findFirst()
                .orElse(key);
    }

    public ChatClient get(String model) {
        ChatClient client = clients.get(model);
        if (client == null) {
            throw new IllegalArgumentException("未知模型: " + model + "，可用: " + clients.keySet());
        }
        return client;
    }

    /**
     * 解析实际生效的模型 key：请求的模型已注册则直接返回；请求的是默认模型而默认模型
     * 未注册（密钥缺失）时回退到首个可用模型（沙箱中即 deterministic）；其余情况抛异常。
     */
    public String resolveModelKey(String requested) {
        if (clients.containsKey(requested)) {
            return requested;
        }
        if (ChatModels.DEFAULT_MODEL.equals(requested) && !clients.isEmpty()) {
            return clients.keySet().iterator().next();
        }
        throw new IllegalArgumentException("未知模型: " + requested + "，可用: " + clients.keySet());
    }

    public ChatClient defaultClient() {
        return clients.values().stream().findFirst().orElse(null);
    }
}
