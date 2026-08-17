package com.huangyangwei.iknow.module.feedback.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huangyangwei.iknow.api.dto.feedback.NotificationItem;
import com.huangyangwei.iknow.common.api.PageResult;
import com.huangyangwei.iknow.common.api.ResultCode;
import com.huangyangwei.iknow.common.constant.Constants;
import com.huangyangwei.iknow.common.exception.BusinessException;
import com.huangyangwei.iknow.module.feedback.entity.SysNotification;
import com.huangyangwei.iknow.module.feedback.mapper.SysNotificationMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 站内通知（技术方案 §4.3「本期最小实现」）：反馈处理结果通知提交人，
 * 用户侧可列表/标记已读。
 */
@Service
public class NotificationService {

    private final SysNotificationMapper notificationMapper;

    public NotificationService(SysNotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    @Transactional
    public void notify(Long userId, String title, String content, Long refId) {
        SysNotification notification = new SysNotification();
        notification.setUserId(userId);
        notification.setTitle(StringUtils.hasText(title) ? title.trim() : "通知");
        notification.setContent(content);
        notification.setType(Constants.NOTIFICATION_TYPE_FEEDBACK);
        notification.setRefId(refId);
        notification.setIsRead(false);
        notificationMapper.insert(notification);
    }

    public PageResult<NotificationItem> pageMy(Long userId, long page, long size, boolean unreadOnly) {
        LambdaQueryWrapper<SysNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNotification::getUserId, userId);
        if (unreadOnly) {
            wrapper.eq(SysNotification::getIsRead, false);
        }
        wrapper.orderByDesc(SysNotification::getCreatedAt);
        Page<SysNotification> result = notificationMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getPages(),
                result.getRecords().stream().map(this::toItem).toList());
    }

    public long unreadCount(Long userId) {
        return notificationMapper.selectCount(new LambdaQueryWrapper<SysNotification>()
                .eq(SysNotification::getUserId, userId)
                .eq(SysNotification::getIsRead, false));
    }

    @Transactional
    public void markRead(Long id, Long userId) {
        SysNotification notification = requireOwned(id, userId);
        if (!Boolean.TRUE.equals(notification.getIsRead())) {
            notification.setIsRead(true);
            notificationMapper.updateById(notification);
        }
    }

    @Transactional
    public void markAllRead(Long userId) {
        List<SysNotification> unread = notificationMapper.selectList(new LambdaQueryWrapper<SysNotification>()
                .eq(SysNotification::getUserId, userId)
                .eq(SysNotification::getIsRead, false));
        for (SysNotification notification : unread) {
            notification.setIsRead(true);
            notificationMapper.updateById(notification);
        }
    }

    private SysNotification requireOwned(Long id, Long userId) {
        SysNotification notification = notificationMapper.selectById(id);
        if (notification == null || !notification.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND, "通知不存在");
        }
        return notification;
    }

    private NotificationItem toItem(SysNotification n) {
        NotificationItem item = new NotificationItem();
        item.setId(n.getId());
        item.setTitle(n.getTitle());
        item.setContent(n.getContent());
        item.setType(n.getType());
        item.setRefId(n.getRefId());
        item.setIsRead(n.getIsRead());
        item.setCreatedAt(n.getCreatedAt());
        return item;
    }
}
