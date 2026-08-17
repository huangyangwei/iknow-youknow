package com.huangyangwei.iknow.module.feedback.controller;

import com.huangyangwei.iknow.api.dto.feedback.NotificationItem;
import com.huangyangwei.iknow.common.api.PageResult;
import com.huangyangwei.iknow.common.api.Result;
import com.huangyangwei.iknow.module.feedback.service.NotificationService;
import com.huangyangwei.iknow.module.knowledge.support.SecurityUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 站内通知接口：当前用户的通知列表 / 未读数 / 标记已读。
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public Result<PageResult<NotificationItem>> list(@RequestParam(defaultValue = "1") long page,
                                                     @RequestParam(defaultValue = "10") long size,
                                                     @RequestParam(defaultValue = "false") boolean unreadOnly) {
        return Result.ok(notificationService.pageMy(SecurityUtils.currentUser().id(), page, size, unreadOnly));
    }

    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        return Result.ok(notificationService.unreadCount(SecurityUtils.currentUser().id()));
    }

    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(id, SecurityUtils.currentUser().id());
        return Result.ok();
    }

    @PutMapping("/read-all")
    public Result<Void> markAllRead() {
        notificationService.markAllRead(SecurityUtils.currentUser().id());
        return Result.ok();
    }
}
