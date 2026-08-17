package com.huangyangwei.iknow.module.ai.service;

import com.huangyangwei.iknow.common.api.ResultCode;
import com.huangyangwei.iknow.common.exception.BusinessException;
import com.huangyangwei.iknow.module.ai.dto.AskRequest;
import com.huangyangwei.iknow.module.ai.dto.ChatSseEvent;
import com.huangyangwei.iknow.module.ai.model.ChatModels;
import com.huangyangwei.iknow.module.ai.model.ModelRouter;
import com.huangyangwei.iknow.module.ai.support.Citation;
import com.huangyangwei.iknow.module.ai.support.ConfidenceEvaluator;
import com.huangyangwei.iknow.module.ai.support.ConfidenceEvaluator.Confidence;
import com.huangyangwei.iknow.module.ai.entity.QaSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RAG 智能问答（技术方案 §4.1）：混合检索 → RetrievalAugmentationAdvisor 上下文注入 +
 * ChatMemory 多轮 + ChatClient.stream() SSE 流式返回；完成后计算置信度并持久化会话。
 * 事件序列：start → delta* → done（answer/model/confidence/sources）。
 */
@Service
public class RagChatService {

    private static final Logger log = LoggerFactory.getLogger(RagChatService.class);

    private static final String SYSTEM_PROMPT =
            "你是企业内部知识库问答助手。仅依据系统提供的参考资料回答，若资料不足请明确说明，不要编造。";

    private static final int CITATION_TOP_N = 6;
    private static final Pattern RATING_PATTERN = Pattern.compile("(0?\\.\\d+|[01])(\\b|$)");

    private final ModelRouter modelRouter;
    private final RetrievalAugmentationAdvisor ragAdvisor;
    private final ChatMemory chatMemory;
    private final HybridRetrievalService retrievalService;
    private final ChatSessionService sessionService;

    public RagChatService(ModelRouter modelRouter, RetrievalAugmentationAdvisor ragAdvisor, ChatMemory chatMemory,
                          HybridRetrievalService retrievalService, ChatSessionService sessionService) {
        this.modelRouter = modelRouter;
        this.ragAdvisor = ragAdvisor;
        this.chatMemory = chatMemory;
        this.retrievalService = retrievalService;
        this.sessionService = sessionService;
    }

    public Flux<ChatSseEvent> ask(AskRequest request, Long userId) {
        String modelKey;
        try {
            // 显式指定不可用模型报错；默认模型密钥缺失时回退到首个可用模型（沙箱中即 deterministic）。
            modelKey = modelRouter.resolveModelKey(request.model());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_FAILED, e.getMessage());
        }
        ChatClient client = modelRouter.get(modelKey);

        QaSession session = sessionService.resolveSession(request.sessionId(), userId, request.question());
        String conversationId = String.valueOf(session.getId());

        List<Citation> citations = retrievalService.retrieve(request.question(), CITATION_TOP_N);
        double maxSimilarity = citations.stream()
                .mapToDouble(c -> c.score() == null ? 0 : c.score())
                .max()
                .orElse(0);
        double citationCoverage = citations.isEmpty() ? 0 : Math.min(1, (double) citations.size() / CITATION_TOP_N);

        StringBuffer answerBuffer = new StringBuffer();
        Flux<ChatSseEvent> deltas = client.prompt()
                .system(SYSTEM_PROMPT)
                .user(request.question())
                .advisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .advisors(ragAdvisor)
                .stream()
                .content()
                .map(chunk -> {
                    answerBuffer.append(chunk);
                    return ChatSseEvent.delta(chunk);
                });

        // 用户消息延迟到流成功完成后持久化：流失败时不留下「有问无答」的孤儿消息（W2）。
        // 多轮记忆走 ChatMemory（内存），不受持久化时机影响，语义不变。
        Mono<ChatSseEvent> done = Mono.fromCallable(() -> {
            String answer = answerBuffer.toString();
            sessionService.addUserMessage(session.getId(), request.question());
            double selfEval = llmSelfEvaluate(modelKey, request.question(), answer, citations);
            Confidence confidence = ConfidenceEvaluator.evaluate(maxSimilarity, citationCoverage, selfEval,
                    citations.size());
            sessionService.saveAssistantAnswer(session.getId(), modelKey, answer, confidence, citations);
            return ChatSseEvent.done(answer, modelKey, modelRouter.displayName(modelKey),
                    confidence, citations, session.getId());
        }).subscribeOn(Schedulers.boundedElastic());

        return Flux.concat(
                Flux.just(ChatSseEvent.start(session.getId())),
                deltas,
                done
        ).onErrorResume(e -> {
            // W3：原始异常（含驱动/DB 细节）只记服务端日志，SSE 客户端只收通用提示。
            log.error("RAG 问答流式处理失败, model={}, sessionId={}, question={}",
                    modelKey, session.getId(), request.question(), e);
            return Flux.just(ChatSseEvent.error("生成回答失败，请稍后重试"));
        });
    }

    /** LLM 自评（技术方案 §5.4 分量 3）：让同一模型对答案打分 0~1，失败回退 0.5。 */
    private double llmSelfEvaluate(String modelKey, String question, String answer, List<Citation> citations) {
        if (answer == null || answer.isBlank()) {
            return 0;
        }
        try {
            String rating = modelRouter.get(modelKey).prompt()
                    .system("你是一个答案质量评估器。只输出一个 0 到 1 之间的数字（可含小数点），"
                            + "表示答案在多大程度上准确回答了问题并合理使用了提供的参考资料。不要输出任何其它内容。")
                    .user("问题：" + question + "\n参考资料数量：" + citations.size() + "\n答案：" + answer)
                    .call()
                    .content();
            return parseRating(rating);
        } catch (Exception e) {
            return 0.5;
        }
    }

    private double parseRating(String text) {
        if (text == null) {
            return 0.5;
        }
        Matcher matcher = RATING_PATTERN.matcher(text);
        if (matcher.find()) {
            try {
                return Math.max(0, Math.min(1, Double.parseDouble(matcher.group(1))));
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        return 0.5;
    }
}
