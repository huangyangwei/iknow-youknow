package com.huangyangwei.iknow.module.ai.controller;

import com.huangyangwei.iknow.common.api.PageResult;
import com.huangyangwei.iknow.common.api.Result;
import com.huangyangwei.iknow.common.api.ResultCode;
import com.huangyangwei.iknow.common.exception.BusinessException;
import com.huangyangwei.iknow.module.ai.dto.AskRequest;
import com.huangyangwei.iknow.module.ai.dto.ChatSseEvent;
import com.huangyangwei.iknow.module.ai.entity.QaMessage;
import com.huangyangwei.iknow.module.ai.entity.QaSession;
import com.huangyangwei.iknow.module.ai.model.ChatModels;
import com.huangyangwei.iknow.module.ai.service.ChatSessionService;
import com.huangyangwei.iknow.module.ai.service.RagChatService;
import com.huangyangwei.iknow.module.knowledge.support.SecurityUtils;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 智能问答接口（技术方案 §7.2）：流式提问（SSE）、会话列表/消息/删除。
 * 所有角色可用（JWT 认证）。
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final RagChatService ragChatService;
    private final ChatSessionService sessionService;

    public ChatController(RagChatService ragChatService, ChatSessionService sessionService) {
        this.ragChatService = ragChatService;
        this.sessionService = sessionService;
    }

    /** 提问（SSE 流式返回：start → delta* → done|error）。 */
    @PostMapping(value = "/ask", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatSseEvent> ask(@RequestBody AskRequest request) {
        if (request == null || !StringUtils.hasText(request.question())) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_FAILED, "问题不能为空");
        }
        String model = StringUtils.hasText(request.model()) ? request.model() : ChatModels.DEFAULT_MODEL;
        AskRequest normalized = new AskRequest(request.sessionId(), model, request.question().trim());
        return ragChatService.ask(normalized, SecurityUtils.currentUser().id());
    }

    /** 当前用户会话列表。 */
    @GetMapping("/sessions")
    public Result<PageResult<QaSession>> sessions(@RequestParam(defaultValue = "1") long page,
                                                  @RequestParam(defaultValue = "10") long size) {
        return Result.ok(sessionService.listSessions(SecurityUtils.currentUser().id(), page, size));
    }

    /** 会话消息（含 user/assistant 与引用 JSON）。 */
    @GetMapping("/sessions/{id}/messages")
    public Result<List<QaMessage>> messages(@PathVariable Long id) {
        return Result.ok(sessionService.listMessages(id, SecurityUtils.currentUser().id()));
    }

    /** 删除会话（连带消息）。 */
    @DeleteMapping("/sessions/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sessionService.deleteSession(id, SecurityUtils.currentUser().id());
        return Result.ok();
    }
}
