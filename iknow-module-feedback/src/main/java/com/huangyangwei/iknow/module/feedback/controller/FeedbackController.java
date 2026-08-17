package com.huangyangwei.iknow.module.feedback.controller;

import com.huangyangwei.iknow.api.dto.feedback.FeedbackCreateRequest;
import com.huangyangwei.iknow.api.dto.feedback.FeedbackHandleRequest;
import com.huangyangwei.iknow.api.dto.feedback.FeedbackItem;
import com.huangyangwei.iknow.common.api.PageResult;
import com.huangyangwei.iknow.common.api.Result;
import com.huangyangwei.iknow.common.constant.Constants;
import com.huangyangwei.iknow.common.security.RequirePermission;
import com.huangyangwei.iknow.module.feedback.service.FeedbackService;
import com.huangyangwei.iknow.module.knowledge.support.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 反馈接口（技术方案 §7.2）：提交赞/踩/纠错/建议（所有角色）、
 * 列表筛选与处理流转（需 feedback:handle 权限，管理员工作台）。
 */
@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    public Result<FeedbackItem> submit(@RequestBody @Valid FeedbackCreateRequest request) {
        return Result.ok(feedbackService.submit(request, SecurityUtils.currentUser().id()));
    }

    @GetMapping
    @RequirePermission(Constants.PERM_FEEDBACK_HANDLE)
    public Result<PageResult<FeedbackItem>> list(@RequestParam(required = false) String status,
                                                 @RequestParam(required = false) String type,
                                                 @RequestParam(defaultValue = "1") long page,
                                                 @RequestParam(defaultValue = "10") long size) {
        return Result.ok(feedbackService.page(status, type, page, size));
    }

    @PutMapping("/{id}/handle")
    @RequirePermission(Constants.PERM_FEEDBACK_HANDLE)
    public Result<FeedbackItem> handle(@PathVariable Long id,
                                       @RequestBody @Valid FeedbackHandleRequest request) {
        return Result.ok(feedbackService.handle(id, request, SecurityUtils.currentUser().id()));
    }
}
