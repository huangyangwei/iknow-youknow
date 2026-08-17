-- P3 反馈闭环模块表结构（技术方案 §9.2）
-- 1. fb_feedback：赞/踩/纠错/建议 + 处理流转（pending → processing → resolved）
-- 2. sys_notification：反馈处理结果站内通知提交人（本期最小实现）

CREATE TABLE fb_feedback (
    id          BIGINT       NOT NULL,
    type        VARCHAR(16)  NOT NULL,
    source_type VARCHAR(16),
    source_id   BIGINT,
    session_id  BIGINT,
    question    TEXT,
    content     TEXT,
    status      VARCHAR(20)  NOT NULL DEFAULT 'pending',
    handler_id  BIGINT,
    handle_note VARCHAR(500),
    handled_at  TIMESTAMP,
    created_by  BIGINT,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_fb_feedback PRIMARY KEY (id)
);

CREATE INDEX idx_feedback_status ON fb_feedback (status, created_at);
CREATE INDEX idx_feedback_type ON fb_feedback (type);
CREATE INDEX idx_feedback_created_by ON fb_feedback (created_by);

CREATE TABLE sys_notification (
    id         BIGINT       NOT NULL,
    user_id    BIGINT       NOT NULL,
    title      VARCHAR(255) NOT NULL,
    content    TEXT,
    type       VARCHAR(32)  NOT NULL DEFAULT 'feedback',
    ref_id     BIGINT,
    is_read    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_sys_notification PRIMARY KEY (id)
);

CREATE INDEX idx_notification_user ON sys_notification (user_id, is_read, created_at);
