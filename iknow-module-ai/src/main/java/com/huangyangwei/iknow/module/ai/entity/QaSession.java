package com.huangyangwei.iknow.module.ai.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.huangyangwei.iknow.common.entity.BaseEntity;

/**
 * 问答会话：一次多轮对话的容器。
 */
@TableName("qa_session")
public class QaSession extends BaseEntity {

    private Long userId;
    private String title;

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
}
