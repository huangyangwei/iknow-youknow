package com.huangyangwei.iknow.module.feedback.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huangyangwei.iknow.api.dto.feedback.FeedbackCreateRequest;
import com.huangyangwei.iknow.api.dto.feedback.FeedbackHandleRequest;
import com.huangyangwei.iknow.api.dto.feedback.FeedbackItem;
import com.huangyangwei.iknow.common.api.PageResult;
import com.huangyangwei.iknow.common.api.ResultCode;
import com.huangyangwei.iknow.common.constant.Constants;
import com.huangyangwei.iknow.common.exception.BusinessException;
import com.huangyangwei.iknow.module.feedback.entity.FbFeedback;
import com.huangyangwei.iknow.module.feedback.mapper.FbFeedbackMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 反馈闭环（技术方案 §4.3）：提交赞/踩/纠错/建议、管理员筛选、处理流转
 * （pending → processing → resolved），处理完成时站内通知提交人。
 */
@Service
public class FeedbackService {

    private static final Set<String> VALID_TYPES = Set.of(
            Constants.FEEDBACK_TYPE_LIKE, Constants.FEEDBACK_TYPE_DISLIKE,
            Constants.FEEDBACK_TYPE_CORRECTION, Constants.FEEDBACK_TYPE_SUGGESTION);

    private static final Set<String> VALID_STATUSES = Set.of(
            Constants.FEEDBACK_STATUS_PENDING, Constants.FEEDBACK_STATUS_PROCESSING,
            Constants.FEEDBACK_STATUS_RESOLVED);

    private final FbFeedbackMapper feedbackMapper;
    private final NotificationService notificationService;

    public FeedbackService(FbFeedbackMapper feedbackMapper, NotificationService notificationService) {
        this.feedbackMapper = feedbackMapper;
        this.notificationService = notificationService;
    }

    public FeedbackItem submit(FeedbackCreateRequest request, Long userId) {
        String type = request.getType().trim();
        if (!VALID_TYPES.contains(type)) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_FAILED,
                    "反馈类型仅支持 like/dislike/correction/suggestion");
        }
        FbFeedback feedback = new FbFeedback();
        feedback.setType(type);
        feedback.setSourceType(trimToNull(request.getSourceType()));
        feedback.setSourceId(request.getSourceId());
        feedback.setSessionId(request.getSessionId());
        feedback.setQuestion(trimToNull(request.getQuestion()));
        feedback.setContent(trimToNull(request.getContent()));
        feedback.setStatus(Constants.FEEDBACK_STATUS_PENDING);
        feedback.setCreatedBy(userId);
        feedbackMapper.insert(feedback);
        return toItem(feedback);
    }

    public PageResult<FeedbackItem> page(String status, String type, long page, long size) {
        LambdaQueryWrapper<FbFeedback> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            wrapper.eq(FbFeedback::getStatus, status.trim());
        }
        if (StringUtils.hasText(type)) {
            wrapper.eq(FbFeedback::getType, type.trim());
        }
        wrapper.orderByDesc(FbFeedback::getCreatedAt);
        Page<FbFeedback> result = feedbackMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getPages(),
                result.getRecords().stream().map(this::toItem).toList());
    }

    @Transactional
    public FeedbackItem handle(Long id, FeedbackHandleRequest request, Long userId) {
        FbFeedback feedback = requireFeedback(id);
        String target = request.getStatus().trim();
        if (!VALID_STATUSES.contains(target) || Constants.FEEDBACK_STATUS_PENDING.equals(target)) {
            throw new BusinessException(ResultCode.FEEDBACK_STATUS_INVALID,
                    "目标状态仅支持 processing/resolved");
        }
        if (!canTransition(feedback.getStatus(), target)) {
            throw new BusinessException(ResultCode.FEEDBACK_STATUS_INVALID,
                    "状态流转不合法：" + feedback.getStatus() + " → " + target);
        }
        feedback.setStatus(target);
        feedback.setHandlerId(userId);
        if (StringUtils.hasText(request.getHandleNote())) {
            feedback.setHandleNote(request.getHandleNote().trim());
        }
        if (Constants.FEEDBACK_STATUS_RESOLVED.equals(target)) {
            feedback.setHandledAt(LocalDateTime.now());
        }
        feedbackMapper.updateById(feedback);

        if (Constants.FEEDBACK_STATUS_RESOLVED.equals(target) && feedback.getCreatedBy() != null) {
            notificationService.notify(feedback.getCreatedBy(),
                    "您的反馈已处理",
                    buildResolveMessage(feedback),
                    feedback.getId());
        }
        return toItem(feedback);
    }

    public FbFeedback requireFeedback(Long id) {
        FbFeedback feedback = feedbackMapper.selectById(id);
        if (feedback == null) {
            throw new BusinessException(ResultCode.FEEDBACK_NOT_FOUND);
        }
        return feedback;
    }

    private boolean canTransition(String current, String target) {
        if (current.equals(target)) {
            return false;
        }
        return switch (current) {
            case Constants.FEEDBACK_STATUS_PENDING ->
                    Constants.FEEDBACK_STATUS_PROCESSING.equals(target)
                            || Constants.FEEDBACK_STATUS_RESOLVED.equals(target);
            case Constants.FEEDBACK_STATUS_PROCESSING ->
                    Constants.FEEDBACK_STATUS_RESOLVED.equals(target);
            default -> false;
        };
    }

    private String buildResolveMessage(FbFeedback feedback) {
        String typeLabel = switch (feedback.getType()) {
            case Constants.FEEDBACK_TYPE_LIKE -> "点赞";
            case Constants.FEEDBACK_TYPE_DISLIKE -> "踩";
            case Constants.FEEDBACK_TYPE_CORRECTION -> "纠错";
            default -> "建议";
        };
        String note = StringUtils.hasText(feedback.getHandleNote())
                ? "处理说明：" + feedback.getHandleNote() : "感谢您的反馈";
        return "您提交的「" + typeLabel + "」反馈（#" + feedback.getId() + "）已处理完毕。" + note;
    }

    private FeedbackItem toItem(FbFeedback f) {
        FeedbackItem item = new FeedbackItem();
        item.setId(f.getId());
        item.setType(f.getType());
        item.setSourceType(f.getSourceType());
        item.setSourceId(f.getSourceId());
        item.setSessionId(f.getSessionId());
        item.setQuestion(f.getQuestion());
        item.setContent(f.getContent());
        item.setStatus(f.getStatus());
        item.setHandlerId(f.getHandlerId());
        item.setHandleNote(f.getHandleNote());
        item.setHandledAt(f.getHandledAt());
        item.setCreatedBy(f.getCreatedBy());
        item.setCreatedAt(f.getCreatedAt());
        item.setUpdatedAt(f.getUpdatedAt());
        return item;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
