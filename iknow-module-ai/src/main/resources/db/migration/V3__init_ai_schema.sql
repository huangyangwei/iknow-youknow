-- P2 AI 模块表结构（技术方案 §9.2）
-- 1. 全文检索查询函数（与 V2 索引侧同一配置解析，保证 FTS 检索与索引分词一致）
-- 2. qa_session / qa_message（问答会话与消息）
-- 3. kb_chunk（RAG 向量块，Spring AI PgVectorStore 标准列 + search_tsv 全文列）
--    kb_chunk 依赖 pgvector extension；嵌入式 PG / 无 pgvector 环境跳过该段（RAG 检索仅生产可用）

CREATE OR REPLACE FUNCTION kb_fts_query(query_text text) RETURNS tsquery
LANGUAGE plpgsql STABLE AS $func$
DECLARE
    cfg regconfig;
BEGIN
    SELECT oid INTO cfg FROM pg_ts_config WHERE cfgname = 'iknow_zhcfg';
    IF cfg IS NULL THEN
        SELECT oid INTO cfg FROM pg_ts_config WHERE cfgname = 'simple';
    END IF;
    RETURN websearch_to_tsquery(cfg, query_text);
END
$func$;

-- 1. 问答会话表
CREATE TABLE qa_session (
    id         BIGINT       NOT NULL,
    user_id    BIGINT       NOT NULL,
    title      VARCHAR(255),
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_qa_session PRIMARY KEY (id)
);

CREATE INDEX idx_qa_session_user ON qa_session (user_id, updated_at);

-- 2. 问答消息表（sources 存引用 JSON 数组文本：[{title,url,knowledgeId,versionNo,chunkText,score}]）
CREATE TABLE qa_message (
    id         BIGINT       NOT NULL,
    session_id BIGINT       NOT NULL,
    role       VARCHAR(16)  NOT NULL,
    content    TEXT,
    model      VARCHAR(64),
    confidence VARCHAR(16),
    sources    TEXT,
    tokens     INT,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_qa_message PRIMARY KEY (id)
);

CREATE INDEX idx_qa_message_session ON qa_message (session_id, created_at);

-- 3. RAG 向量块 kb_chunk（pgvector 可用才建）
DO $ai$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_available_extensions WHERE name = 'vector') THEN
        EXECUTE $ddl$CREATE EXTENSION IF NOT EXISTS vector$ddl$;

        EXECUTE $ddl$
        CREATE TABLE IF NOT EXISTS kb_chunk (
            id         VARCHAR(64) PRIMARY KEY,
            content    TEXT,
            metadata   JSON,
            embedding  vector(1024),
            search_tsv tsvector
        )
        $ddl$;

        -- search_tsv 由表触发器维护（复用 V2 的配置解析函数，支持 zhparser 可选）
        EXECUTE $ddl$
        CREATE OR REPLACE FUNCTION kb_chunk_search_tsv_trigger() RETURNS trigger AS $func$
        BEGIN
            NEW.search_tsv := kb_knowledge_tsv('', NEW.content);
            RETURN NEW;
        END
        $func$ LANGUAGE plpgsql
        $ddl$;

        EXECUTE $ddl$
        CREATE TRIGGER trg_kb_chunk_search_tsv
            BEFORE INSERT OR UPDATE OF content ON kb_chunk
            FOR EACH ROW EXECUTE FUNCTION kb_chunk_search_tsv_trigger()
        $ddl$;

        -- FTS 检索（kb_chunk 全文通道）与 HNSW 余弦索引（M0 约束 vector_cosine_ops）
        EXECUTE $ddl$
        CREATE INDEX IF NOT EXISTS idx_kb_chunk_tsv_gin ON kb_chunk USING GIN (search_tsv)
        $ddl$;

        EXECUTE $ddl$
        CREATE INDEX IF NOT EXISTS idx_kb_chunk_hnsw ON kb_chunk USING hnsw (embedding vector_cosine_ops)
        $ddl$;
    END IF;
END
$ai$;
