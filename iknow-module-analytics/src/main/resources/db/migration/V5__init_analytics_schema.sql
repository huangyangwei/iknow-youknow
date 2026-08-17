-- P3 数据分析模块表结构（技术方案 §9.2）
-- stat_query_log：搜索/问答日志，仪表盘聚合源（热门搜索/无结果率/查询趋势）

CREATE TABLE stat_query_log (
    id         BIGINT      NOT NULL,
    user_id    BIGINT,
    query_type VARCHAR(8)  NOT NULL,
    keyword    VARCHAR(255),
    has_result BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_stat_query_log PRIMARY KEY (id)
);

CREATE INDEX idx_stat_time ON stat_query_log (created_at, query_type);
