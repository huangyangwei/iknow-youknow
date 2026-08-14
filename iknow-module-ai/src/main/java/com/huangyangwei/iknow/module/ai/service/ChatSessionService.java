package com.huangyangwei.iknow.module.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huangyangwei.iknow.common.api.PageResult;
import com.huangyangwei.iknow.common.api.ResultCode;
import com.huangyangwei.iknow.common.exception.BusinessException;
import com.huangyangwei.iknow.module.ai.entity.QaMessage;
import com.huangyangwei.iknow.module.ai.entity.QaSession;
import com.huangyangwei.iknow.module.ai.mapper.QaMessageMapper;
import com.huangyangwei.iknow.module.ai.mapper.QaSessionMapper;
import com.huangyangwei.iknow.module.ai.support.Citation;
import com.huangyangwei.iknow.module.ai.support.ConfidenceEvaluator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * 问答会话/消息持久化（技术方案 §7.2）：qa_session + qa_message。
 * 会话归属当前用户，跨会话读取做属主校验。
 */
@Service
public class ChatSessionService {

    private static final int TITLE_MAX_LENGTH = 30;

    private final QaSessionMapper sessionMapper;
    private final QaMessageMapper messageMapper;
    private final ObjectMapper objectMapper;

    public ChatSessionService(QaSessionMapper sessionMapper, QaMessageMapper messageMapper, ObjectMapper objectMapper) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.objectMapper = objectMapper;
    }

    /** 会话不存在则新建（标题取问题前 30 字）。 */
    @Transactional
    public QaSession resolveSession(Long sessionId, Long userId, String question) {
        if (sessionId != null) {
            return requireOwned(sessionId, userId);
        }
        QaSession session = new QaSession();
        session.setUserId(userId);
        String title = question == null ? "" : question.trim();
        session.setTitle(title.length() > TITLE_MAX_LENGTH ? title.substring(0, TITLE_MAX_LENGTH) : title);
        sessionMapper.insert(session);
        return session;
    }

    public PageResult<QaSession> listSessions(Long userId, long page, long size) {
        Page<QaSession> result = sessionMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<QaSession>()
                        .eq(QaSession::getUserId, userId)
                        .orderByDesc(QaSession::getUpdatedAt));
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getPages(),
                result.getRecords());
    }

    public List<QaMessage> listMessages(Long sessionId, Long userId) {
        requireOwned(sessionId, userId);
        return messageMapper.selectList(new LambdaQueryWrapper<QaMessage>()
                .eq(QaMessage::getSessionId, sessionId)
                .orderByAsc(QaMessage::getCreatedAt));
    }

    @Transactional
    public void deleteSession(Long sessionId, Long userId) {
        requireOwned(sessionId, userId);
        messageMapper.delete(new LambdaQueryWrapper<QaMessage>()
                .eq(QaMessage::getSessionId, sessionId));
        sessionMapper.deleteById(sessionId);
    }

    @Transactional
    public void addUserMessage(Long sessionId, String content) {
        QaMessage message = new QaMessage();
        message.setSessionId(sessionId);
        message.setRole("user");
        message.setContent(content);
        messageMapper.insert(message);
        touchSession(sessionId);
    }

    @Transactional
    public void saveAssistantAnswer(Long sessionId, String model, String answer,
                                    ConfidenceEvaluator.Confidence confidence, List<Citation> citations) {
        QaMessage message = new QaMessage();
        message.setSessionId(sessionId);
        message.setRole("assistant");
        message.setContent(answer);
        message.setModel(model);
        message.setConfidence(confidence.level());
        message.setSources(serializeSources(citations));
        messageMapper.insert(message);
        touchSession(sessionId);
    }

    private void touchSession(Long sessionId) {
        QaSession session = sessionMapper.selectById(sessionId);
        if (session != null) {
            session.setUpdatedAt(java.time.LocalDateTime.now());
            sessionMapper.updateById(session);
        }
    }

    private String serializeSources(List<Citation> citations) {
        try {
            return objectMapper.writeValueAsString(citations);
        } catch (Exception e) {
            return "[]";
        }
    }

    public QaSession requireOwned(Long sessionId, Long userId) {
        QaSession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND, "会话不存在");
        }
        return session;
    }
}
