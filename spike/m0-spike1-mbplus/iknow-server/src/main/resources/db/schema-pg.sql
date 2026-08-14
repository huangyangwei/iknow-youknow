-- Spike ① schema（PostgreSQL 16）
-- 说明：spike 演示表，对应生产 kb_knowledge 表结构的精简版（仅保留验证所需列）。
CREATE TABLE IF NOT EXISTS kb_knowledge (
    id          BIGINT PRIMARY KEY,
    title       VARCHAR(255) NOT NULL,
    plain_text  TEXT,
    status      VARCHAR(20)  DEFAULT 'draft',
    created_at  TIMESTAMP    DEFAULT now()
);
