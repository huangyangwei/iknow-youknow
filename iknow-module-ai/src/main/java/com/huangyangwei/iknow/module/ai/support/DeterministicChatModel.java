package com.huangyangwei.iknow.module.ai.support;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 本地确定性 ChatModel（沙箱演示桩，与 DeterministicEmbeddingModel 配套）：
 * 不访问外部 LLM，根据最后一条用户消息生成固定模板回答并按 12 字符分片流式输出。
 * 用于在无 API Key 环境中验证「提问 → 混合检索 → RAG → SSE 流式 → 置信度 → 会话持久化」
 * 完整链路。生产环境由真实模型替代。
 */
public class DeterministicChatModel implements ChatModel {

    private static final int CHUNK_SIZE = 12;

    @Override
    public ChatResponse call(Prompt prompt) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(answer(prompt)))));
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        String answer = answer(prompt);
        return Flux.fromArray(splitChunks(answer))
                .map(chunk -> new ChatResponse(List.of(new Generation(new AssistantMessage(chunk)))));
    }

    private String answer(Prompt prompt) {
        String question = lastUserText(prompt);
        if (question == null || question.isBlank()) {
            question = "（空问题）";
        }
        if (question.length() > 80) {
            question = question.substring(0, 80) + "…";
        }
        return "（本地确定性模型演示回答）已对「" + question + "」完成检索，"
                + "下列引用为命中的知识片段，请以引用内容为准。";
    }

    private String lastUserText(Prompt prompt) {
        List<Message> messages = prompt.getInstructions();
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message message = messages.get(i);
            if (message.getMessageType() == MessageType.USER) {
                return message.getText();
            }
        }
        return null;
    }

    private String[] splitChunks(String text) {
        if (text == null || text.isEmpty()) {
            return new String[]{""};
        }
        int length = text.length();
        String[] chunks = new String[(length + CHUNK_SIZE - 1) / CHUNK_SIZE];
        int index = 0;
        for (int i = 0; i < length; i += CHUNK_SIZE) {
            chunks[index++] = text.substring(i, Math.min(length, i + CHUNK_SIZE));
        }
        return chunks;
    }
}
