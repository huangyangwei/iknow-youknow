package com.huangyangwei.iknow.spike1.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * Spike ① 实体：验证 @TableName / @TableId 基础能力。
 * 对应真实环境中的 kb_knowledge 表（DDL 见 iknow-server/src/main/resources/db/schema-pg.sql）。
 */
@TableName("kb_knowledge")
public class KbKnowledge {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String title;

    private String plainText;

    private String status;

    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPlainText() {
        return plainText;
    }

    public void setPlainText(String plainText) {
        this.plainText = plainText;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
