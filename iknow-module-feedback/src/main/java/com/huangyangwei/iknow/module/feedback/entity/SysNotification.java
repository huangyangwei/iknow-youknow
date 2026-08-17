package com.huangyangwei.iknow.module.feedback.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.huangyangwei.iknow.common.entity.BaseEntity;

/**
 * 站内通知：反馈处理结果通知提交人（本期最小实现）。
 */
@TableName("sys_notification")
public class SysNotification extends BaseEntity {

    private Long userId;
    private String title;
    private String content;
    private String type;
    private Long refId;
    private Boolean isRead;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getRefId() {
        return refId;
    }

    public void setRefId(Long refId) {
        this.refId = refId;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }
}
