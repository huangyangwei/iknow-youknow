package com.huangyangwei.iknow.spike2.web;

import com.huangyangwei.iknow.spike2.rag.ModelRouter;
import com.huangyangwei.iknow.spike2.rag.RagService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * RAG Demo HTTP 接口：模型列表 + 流式问答（SSE）。
 */
@RestController
@RequestMapping("/api/chat")
public class RagController {

    private final RagService ragService;
    private final ModelRouter modelRouter;

    public RagController(RagService ragService, ModelRouter modelRouter) {
        this.ragService = ragService;
        this.modelRouter = modelRouter;
    }

    /** 已注册模型列表 */
    @GetMapping("/models")
    public Map<String, List<String>> models() {
        return Map.of("models", modelRouter.availableModels());
    }

    /** RAG 流式问答（SSE，text/event-stream） */
    @PostMapping(value = "/ask", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> ask(@RequestBody AskRequest request) {
        return ragService.askStream(request.model(), request.question());
    }

    /** 非流式问答 */
    @PostMapping("/ask/sync")
    public Map<String, String> askSync(@RequestBody AskRequest request) {
        return Map.of("answer", ragService.ask(request.model(), request.question()));
    }

    public record AskRequest(String model, String question) {
    }
}
