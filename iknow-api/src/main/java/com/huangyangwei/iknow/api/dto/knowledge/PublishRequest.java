package com.huangyangwei.iknow.api.dto.knowledge;

import java.time.LocalDateTime;

/**
 * 发布请求：scheduledTime 为空/过去时立即发布；为未来时间时进入 pending_publish 定时发布队列。
 */
public class PublishRequest {

    private LocalDateTime scheduledTime;

    private String changeNote;

    public LocalDateTime getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(LocalDateTime scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public String getChangeNote() {
        return changeNote;
    }

    public void setChangeNote(String changeNote) {
        this.changeNote = changeNote;
    }
}
