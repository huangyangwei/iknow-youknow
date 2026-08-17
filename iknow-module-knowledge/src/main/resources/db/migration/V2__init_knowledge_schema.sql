-- 知识管理模块表结构（技术方案 §9.2）+ 全文索引（tsvector + GIN，zhparser 可选）

-- 1. 分类表（树形：parent_id=0 为一级，path 存血缘）
CREATE TABLE kb_category (
    id           BIGINT      NOT NULL,
    parent_id    BIGINT      NOT NULL DEFAULT 0,
    name         VARCHAR(64) NOT NULL,
    product_line VARCHAR(64),
    sort         INT         NOT NULL DEFAULT 0,
    level        INT         NOT NULL DEFAULT 1,
    path         VARCHAR(500),
    created_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_kb_category PRIMARY KEY (id),
    CONSTRAINT uk_kb_category_name UNIQUE (name)
);

CREATE INDEX idx_kb_category_parent ON kb_category (parent_id);

-- 2. 标签表
CREATE TABLE kb_tag (
    id         BIGINT      NOT NULL,
    name       VARCHAR(64) NOT NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_kb_tag PRIMARY KEY (id),
    CONSTRAINT uk_kb_tag_name UNIQUE (name)
);

-- 3. 知识表（html_content 展示通道 / plain_text 检索通道）
CREATE TABLE kb_knowledge (
    id                     BIGINT        NOT NULL,
    title                  VARCHAR(255)  NOT NULL,
    html_content           TEXT,
    plain_text             TEXT,
    summary                VARCHAR(500),
    category_id            BIGINT,
    knowledge_type         VARCHAR(32)   NOT NULL DEFAULT 'FAQ',
    status                 VARCHAR(32)   NOT NULL DEFAULT 'draft',
    version_no             INT           NOT NULL DEFAULT 1,
    publish_time           TIMESTAMP,
    scheduled_publish_time TIMESTAMP,
    view_count             INT           NOT NULL DEFAULT 0,
    like_count             INT           NOT NULL DEFAULT 0,
    created_by             BIGINT,
    updated_by             BIGINT,
    created_at             TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_kb_knowledge PRIMARY KEY (id)
);

CREATE INDEX idx_kb_knowledge_category ON kb_knowledge (category_id);
CREATE INDEX idx_kb_knowledge_status ON kb_knowledge (status);
CREATE INDEX idx_kb_knowledge_publish_time ON kb_knowledge (publish_time);

-- 4. 版本表（每次发布/回滚生成快照）
CREATE TABLE kb_knowledge_version (
    id           BIGINT       NOT NULL,
    knowledge_id BIGINT       NOT NULL,
    version_no   INT          NOT NULL,
    title        VARCHAR(255) NOT NULL,
    html_content TEXT,
    plain_text   TEXT,
    summary      VARCHAR(500),
    change_note  VARCHAR(500),
    created_by   BIGINT,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_kb_knowledge_version PRIMARY KEY (id),
    CONSTRAINT uk_kb_knowledge_version UNIQUE (knowledge_id, version_no)
);

-- 5. 知识-标签关联
CREATE TABLE kb_knowledge_tag (
    id           BIGINT    NOT NULL,
    knowledge_id BIGINT    NOT NULL,
    tag_id       BIGINT    NOT NULL,
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_kb_knowledge_tag PRIMARY KEY (id),
    CONSTRAINT uk_kb_knowledge_tag UNIQUE (knowledge_id, tag_id)
);

CREATE INDEX idx_kb_knowledge_tag_tag ON kb_knowledge_tag (tag_id);

-- 6. 全文索引：tsvector + GIN。zhparser（第三方中文分词）可用则启用 iknow_zhcfg，
--    否则降级内置 simple 分词，保证嵌入式 PG / 无 zhparser 环境仍可构建。
CREATE OR REPLACE FUNCTION kb_knowledge_tsv(title text, body text) RETURNS tsvector
LANGUAGE plpgsql STABLE AS $$
DECLARE
    cfg regconfig;
BEGIN
    SELECT oid INTO cfg FROM pg_ts_config WHERE cfgname = 'iknow_zhcfg';
    IF cfg IS NULL THEN
        SELECT oid INTO cfg FROM pg_ts_config WHERE cfgname = 'simple';
    END IF;
    RETURN to_tsvector(cfg, COALESCE(title, '') || ' ' || COALESCE(body, ''));
END
$$;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_available_extensions WHERE name = 'zhparser') THEN
        CREATE EXTENSION IF NOT EXISTS zhparser;
        IF NOT EXISTS (SELECT 1 FROM pg_ts_config WHERE cfgname = 'iknow_zhcfg') THEN
            CREATE TEXT SEARCH CONFIGURATION iknow_zhcfg (PARSER = zhparser);
            ALTER TEXT SEARCH CONFIGURATION iknow_zhcfg ADD MAPPING FOR n,v,a,i,e,l WITH simple;
        END IF;
    END IF;
END
$$;

ALTER TABLE kb_knowledge ADD COLUMN search_tsv tsvector;

CREATE OR REPLACE FUNCTION kb_knowledge_search_tsv_trigger() RETURNS trigger AS $$
BEGIN
    NEW.search_tsv := kb_knowledge_tsv(NEW.title, NEW.plain_text);
    RETURN NEW;
END
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_kb_knowledge_search_tsv
    BEFORE INSERT OR UPDATE OF title, plain_text ON kb_knowledge
    FOR EACH ROW EXECUTE FUNCTION kb_knowledge_search_tsv_trigger();

CREATE INDEX idx_kb_knowledge_tsv_gin ON kb_knowledge USING GIN (search_tsv);
