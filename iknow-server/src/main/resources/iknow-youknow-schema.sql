-- =====================================================================
-- iknow-youknow 企业知识库问答系统 —— 完整建表语句（DDL）
--
-- 说明：
--   1. 主键 id 为 BIGINT，由 MyBatis-Plus IdType.ASSIGN_ID（雪花 ID）在应用侧生成，
--      不使用数据库自增序列。
--   2. 与实体类一一对应：BaseEntity(id/created_at/updated_at) + 各模块实体字段。
--   3. 表顺序遵循外键依赖（auth → knowledge → ai → feedback → analytics）。
--   4. kb_chunk（RAG 向量块）依赖 pgvector 扩展，仅在扩展可用时创建（见文末）。
--   5. 全文检索使用 PostgreSQL tsvector + GIN，zhparser 可用时启用中文分词。
--
-- 模块归属：
--   auth      : sys_user / sys_role / sys_user_role / sys_role_permission
--   knowledge : kb_category / kb_tag / kb_knowledge / kb_knowledge_version / kb_knowledge_tag
--   ai        : qa_session / qa_message / kb_chunk(可选)
--   feedback  : fb_feedback / sys_notification
--   analytics : stat_query_log
-- =====================================================================

-- ---------------------------------------------------------------------
-- 认证模块（V1）
-- ---------------------------------------------------------------------

-- 系统用户
CREATE TABLE sys_user (
    id         BIGINT       NOT NULL,
    username   VARCHAR(64)  NOT NULL,
    email      VARCHAR(128) NOT NULL,
    password   VARCHAR(128) NOT NULL,             -- BCrypt 哈希
    nickname   VARCHAR(64),
    avatar     VARCHAR(255),
    status     SMALLINT     NOT NULL DEFAULT 1,   -- 1=正常 0=禁用
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_sys_user PRIMARY KEY (id),
    CONSTRAINT uk_sys_user_username UNIQUE (username),
    CONSTRAINT uk_sys_user_email UNIQUE (email)
);

COMMENT ON TABLE  sys_user IS '系统用户';
COMMENT ON COLUMN sys_user.id         IS '用户 ID（雪花）';
COMMENT ON COLUMN sys_user.username   IS '登录名，唯一';
COMMENT ON COLUMN sys_user.email      IS '邮箱，唯一';
COMMENT ON COLUMN sys_user.password   IS '密码（BCrypt）';
COMMENT ON COLUMN sys_user.nickname   IS '昵称';
COMMENT ON COLUMN sys_user.avatar     IS '头像 URL';
COMMENT ON COLUMN sys_user.status     IS '状态：1=正常 0=禁用';

-- 系统角色
CREATE TABLE sys_role (
    id          BIGINT       NOT NULL,
    code        VARCHAR(64)  NOT NULL,
    name        VARCHAR(64)  NOT NULL,
    description VARCHAR(255),
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_sys_role PRIMARY KEY (id),
    CONSTRAINT uk_sys_role_code UNIQUE (code)
);

COMMENT ON TABLE  sys_role IS '系统角色';
COMMENT ON COLUMN sys_role.code        IS '角色码（ADMIN/EDITOR/MEMBER），唯一';
COMMENT ON COLUMN sys_role.name        IS '角色名';
COMMENT ON COLUMN sys_role.description IS '角色描述';

-- 用户-角色关联
CREATE TABLE sys_user_role (
    id         BIGINT    NOT NULL,
    user_id    BIGINT    NOT NULL,
    role_id    BIGINT    NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_sys_user_role PRIMARY KEY (id),
    CONSTRAINT uk_sys_user_role UNIQUE (user_id, role_id),
    CONSTRAINT fk_sys_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_sys_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role (id) ON DELETE CASCADE
);

COMMENT ON TABLE  sys_user_role IS '用户-角色关联';
COMMENT ON COLUMN sys_user_role.user_id IS '用户 ID → sys_user.id';
COMMENT ON COLUMN sys_user_role.role_id IS '角色 ID → sys_role.id';

-- 角色-权限关联（permission 为 RBAC 权限点字符串）
CREATE TABLE sys_role_permission (
    id         BIGINT      NOT NULL,
    role_id    BIGINT      NOT NULL,
    permission VARCHAR(64) NOT NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_sys_role_permission PRIMARY KEY (id),
    CONSTRAINT uk_sys_role_permission UNIQUE (role_id, permission),
    CONSTRAINT fk_sys_role_permission_role FOREIGN KEY (role_id) REFERENCES sys_role (id) ON DELETE CASCADE
);

COMMENT ON TABLE  sys_role_permission IS '角色-权限关联（权限点字符串）';
COMMENT ON COLUMN sys_role_permission.role_id    IS '角色 ID → sys_role.id';
COMMENT ON COLUMN sys_role_permission.permission IS '权限点，如 knowledge:create';

-- ---------------------------------------------------------------------
-- 知识管理模块（V2）
-- ---------------------------------------------------------------------

-- 分类表（树形：parent_id=0 为一级分类，path 存血缘）
CREATE TABLE kb_category (
    id           BIGINT      NOT NULL,
    parent_id    BIGINT      NOT NULL DEFAULT 0,   -- 0=根（逻辑关联，不设外键）
    name         VARCHAR(64) NOT NULL,
    product_line VARCHAR(64),
    sort         INT         NOT NULL DEFAULT 0,
    level        INT         NOT NULL DEFAULT 1,
    path         VARCHAR(500),                     -- 形如 /1/2
    created_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_kb_category PRIMARY KEY (id),
    CONSTRAINT uk_kb_category_name UNIQUE (name)
);

CREATE INDEX idx_kb_category_parent ON kb_category (parent_id);

COMMENT ON TABLE  kb_category IS '知识分类（树形）';
COMMENT ON COLUMN kb_category.parent_id    IS '父分类 ID，0=一级分类';
COMMENT ON COLUMN kb_category.name         IS '分类名，唯一';
COMMENT ON COLUMN kb_category.product_line IS '所属产品线';
COMMENT ON COLUMN kb_category.sort         IS '排序值，越小越靠前';
COMMENT ON COLUMN kb_category.level        IS '层级，1=一级';
COMMENT ON COLUMN kb_category.path         IS '血缘路径，如 /1/2';

-- 标签表
CREATE TABLE kb_tag (
    id         BIGINT      NOT NULL,
    name       VARCHAR(64) NOT NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_kb_tag PRIMARY KEY (id),
    CONSTRAINT uk_kb_tag_name UNIQUE (name)
);

COMMENT ON TABLE  kb_tag IS '知识标签';
COMMENT ON COLUMN kb_tag.name IS '标签名，唯一';

-- 知识条目（html_content 展示通道 / plain_text 检索通道）
CREATE TABLE kb_knowledge (
    id                     BIGINT        NOT NULL,
    title                  VARCHAR(255)  NOT NULL,
    html_content           TEXT,                    -- 展示通道
    plain_text             TEXT,                    -- 检索通道
    summary                VARCHAR(500),
    category_id            BIGINT,                  -- 可空
    knowledge_type         VARCHAR(32)   NOT NULL DEFAULT 'FAQ',
    status                 VARCHAR(32)   NOT NULL DEFAULT 'draft',
    version_no             INT           NOT NULL DEFAULT 1,
    publish_time           TIMESTAMP,
    scheduled_publish_time TIMESTAMP,
    view_count             INT           NOT NULL DEFAULT 0,
    like_count             INT           NOT NULL DEFAULT 0,
    created_by             BIGINT,
    updated_by             BIGINT,
    search_tsv             tsvector,                -- 全文检索列，触发器维护
    created_at             TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_kb_knowledge PRIMARY KEY (id),
    CONSTRAINT fk_kb_knowledge_category FOREIGN KEY (category_id) REFERENCES kb_category (id) ON DELETE SET NULL,
    CONSTRAINT fk_kb_knowledge_created_by FOREIGN KEY (created_by) REFERENCES sys_user (id) ON DELETE SET NULL,
    CONSTRAINT fk_kb_knowledge_updated_by FOREIGN KEY (updated_by) REFERENCES sys_user (id) ON DELETE SET NULL
);

CREATE INDEX idx_kb_knowledge_category     ON kb_knowledge (category_id);
CREATE INDEX idx_kb_knowledge_status       ON kb_knowledge (status);
CREATE INDEX idx_kb_knowledge_publish_time ON kb_knowledge (publish_time);

COMMENT ON TABLE  kb_knowledge IS '知识条目';
COMMENT ON COLUMN kb_knowledge.title                  IS '标题';
COMMENT ON COLUMN kb_knowledge.html_content           IS 'HTML 内容（展示通道）';
COMMENT ON COLUMN kb_knowledge.plain_text             IS '纯文本（检索通道，发布时由 HTML 解析生成）';
COMMENT ON COLUMN kb_knowledge.summary                IS '摘要';
COMMENT ON COLUMN kb_knowledge.category_id            IS '分类 ID → kb_category.id';
COMMENT ON COLUMN kb_knowledge.knowledge_type         IS '类型（FAQ/DOC 等），默认 FAQ';
COMMENT ON COLUMN kb_knowledge.status                 IS '状态：draft/published/archived/pending_publish';
COMMENT ON COLUMN kb_knowledge.version_no             IS '当前版本号';
COMMENT ON COLUMN kb_knowledge.publish_time           IS '发布时间';
COMMENT ON COLUMN kb_knowledge.scheduled_publish_time IS '定时发布时间';
COMMENT ON COLUMN kb_knowledge.view_count             IS '浏览量';
COMMENT ON COLUMN kb_knowledge.like_count             IS '点赞量';
COMMENT ON COLUMN kb_knowledge.created_by             IS '创建人 → sys_user.id';
COMMENT ON COLUMN kb_knowledge.updated_by             IS '更新人 → sys_user.id';
COMMENT ON COLUMN kb_knowledge.search_tsv             IS '全文检索 tsvector（触发器维护）';

-- 版本表（每次发布/回滚生成快照）
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
    CONSTRAINT uk_kb_knowledge_version UNIQUE (knowledge_id, version_no),
    CONSTRAINT fk_kb_knowledge_version_knowledge FOREIGN KEY (knowledge_id) REFERENCES kb_knowledge (id) ON DELETE CASCADE,
    CONSTRAINT fk_kb_knowledge_version_created_by FOREIGN KEY (created_by) REFERENCES sys_user (id) ON DELETE SET NULL
);

COMMENT ON TABLE  kb_knowledge_version IS '知识版本快照';
COMMENT ON COLUMN kb_knowledge_version.knowledge_id IS '知识 ID → kb_knowledge.id';
COMMENT ON COLUMN kb_knowledge_version.version_no   IS '版本号，与 knowledge_id 联合唯一';
COMMENT ON COLUMN kb_knowledge_version.change_note  IS '变更说明';
COMMENT ON COLUMN kb_knowledge_version.created_by   IS '创建人 → sys_user.id';

-- 知识-标签关联
CREATE TABLE kb_knowledge_tag (
    id           BIGINT    NOT NULL,
    knowledge_id BIGINT    NOT NULL,
    tag_id       BIGINT    NOT NULL,
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_kb_knowledge_tag PRIMARY KEY (id),
    CONSTRAINT uk_kb_knowledge_tag UNIQUE (knowledge_id, tag_id),
    CONSTRAINT fk_kb_knowledge_tag_knowledge FOREIGN KEY (knowledge_id) REFERENCES kb_knowledge (id) ON DELETE CASCADE,
    CONSTRAINT fk_kb_knowledge_tag_tag FOREIGN KEY (tag_id) REFERENCES kb_tag (id) ON DELETE CASCADE
);

CREATE INDEX idx_kb_knowledge_tag_tag ON kb_knowledge_tag (tag_id);

COMMENT ON TABLE  kb_knowledge_tag IS '知识-标签关联';
COMMENT ON COLUMN kb_knowledge_tag.knowledge_id IS '知识 ID → kb_knowledge.id';
COMMENT ON COLUMN kb_knowledge_tag.tag_id       IS '标签 ID → kb_tag.id';

-- ---------------------------------------------------------------------
-- 全文检索辅助函数 / 触发器（V2 + V3）
--   配置解析：优先 iknow_zhcfg（zhparser 中文分词），否则降级 simple。
-- ---------------------------------------------------------------------

-- 由标题 + 正文生成 tsvector（与检索侧共用同一配置，保证分词一致）
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

-- zhparser 可用则启用中文分词配置
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

-- kb_knowledge 全文检索列触发器
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

-- 检索侧查询函数：将用户输入解析为 tsquery（与索引侧配置一致）
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

-- ---------------------------------------------------------------------
-- AI 模块（V3）
-- ---------------------------------------------------------------------

-- 问答会话
CREATE TABLE qa_session (
    id         BIGINT       NOT NULL,
    user_id    BIGINT       NOT NULL,
    title      VARCHAR(255),
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_qa_session PRIMARY KEY (id),
    CONSTRAINT fk_qa_session_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE
);

CREATE INDEX idx_qa_session_user ON qa_session (user_id, updated_at);

COMMENT ON TABLE  qa_session IS '问答会话（一次多轮对话的容器）';
COMMENT ON COLUMN qa_session.user_id IS '用户 ID → sys_user.id';
COMMENT ON COLUMN qa_session.title   IS '会话标题';

-- 问答消息（sources 存引用 JSON 数组文本：[{title,url,knowledgeId,versionNo,chunkText,score}]）
CREATE TABLE qa_message (
    id         BIGINT      NOT NULL,
    session_id BIGINT      NOT NULL,
    role       VARCHAR(16) NOT NULL,               -- user / assistant
    content    TEXT,
    model      VARCHAR(64),
    confidence VARCHAR(16),                        -- high / medium / low
    sources    TEXT,                               -- 引用 JSON 数组文本
    tokens     INT,
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_qa_message PRIMARY KEY (id),
    CONSTRAINT fk_qa_message_session FOREIGN KEY (session_id) REFERENCES qa_session (id) ON DELETE CASCADE
);

CREATE INDEX idx_qa_message_session ON qa_message (session_id, created_at);

COMMENT ON TABLE  qa_message IS '问答消息（会话内一条 user/assistant 消息）';
COMMENT ON COLUMN qa_message.session_id IS '会话 ID → qa_session.id';
COMMENT ON COLUMN qa_message.role       IS '角色：user/assistant';
COMMENT ON COLUMN qa_message.content    IS '消息内容';
COMMENT ON COLUMN qa_message.model      IS '使用的模型';
COMMENT ON COLUMN qa_message.confidence IS '置信度等级：high/medium/low';
COMMENT ON COLUMN qa_message.sources    IS '引用来源 JSON 数组文本';
COMMENT ON COLUMN qa_message.tokens     IS '消耗 token 数';

-- ---------------------------------------------------------------------
-- 反馈闭环模块（V4）
-- ---------------------------------------------------------------------

-- 反馈记录：赞/踩/纠错/建议 + 处理流转（pending → processing → resolved）
CREATE TABLE fb_feedback (
    id          BIGINT      NOT NULL,
    type        VARCHAR(16) NOT NULL,              -- like/dislike/correction/suggestion
    source_type VARCHAR(16),                       -- 来源类型（knowledge/qa 等）
    source_id   BIGINT,                            -- 多态来源 ID（逻辑关联，不设外键）
    session_id  BIGINT,
    question    TEXT,
    content     TEXT,
    status      VARCHAR(20) NOT NULL DEFAULT 'pending',
    handler_id  BIGINT,
    handle_note VARCHAR(500),
    handled_at  TIMESTAMP,
    created_by  BIGINT,
    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_fb_feedback PRIMARY KEY (id),
    CONSTRAINT fk_fb_feedback_session FOREIGN KEY (session_id) REFERENCES qa_session (id) ON DELETE SET NULL,
    CONSTRAINT fk_fb_feedback_handler FOREIGN KEY (handler_id) REFERENCES sys_user (id) ON DELETE SET NULL,
    CONSTRAINT fk_fb_feedback_created_by FOREIGN KEY (created_by) REFERENCES sys_user (id) ON DELETE SET NULL
);

CREATE INDEX idx_feedback_status     ON fb_feedback (status, created_at);
CREATE INDEX idx_feedback_type       ON fb_feedback (type);
CREATE INDEX idx_feedback_created_by ON fb_feedback (created_by);

COMMENT ON TABLE  fb_feedback IS '反馈记录（赞/踩/纠错/建议）';
COMMENT ON COLUMN fb_feedback.type        IS '类型：like/dislike/correction/suggestion';
COMMENT ON COLUMN fb_feedback.source_type IS '来源类型（knowledge/qa 等）';
COMMENT ON COLUMN fb_feedback.source_id   IS '多态来源 ID（配合 source_type）';
COMMENT ON COLUMN fb_feedback.session_id  IS '关联会话 → qa_session.id（可空）';
COMMENT ON COLUMN fb_feedback.question    IS '相关问题';
COMMENT ON COLUMN fb_feedback.content     IS '反馈内容';
COMMENT ON COLUMN fb_feedback.status      IS '状态：pending/processing/resolved';
COMMENT ON COLUMN fb_feedback.handler_id  IS '处理人 → sys_user.id';
COMMENT ON COLUMN fb_feedback.handle_note IS '处理备注';
COMMENT ON COLUMN fb_feedback.handled_at  IS '处理时间';
COMMENT ON COLUMN fb_feedback.created_by  IS '提交人 → sys_user.id';

-- 站内通知
CREATE TABLE sys_notification (
    id         BIGINT       NOT NULL,
    user_id    BIGINT       NOT NULL,
    title      VARCHAR(255) NOT NULL,
    content    TEXT,
    type       VARCHAR(32)  NOT NULL DEFAULT 'feedback',
    ref_id     BIGINT,                            -- 多态关联 ID（逻辑关联，不设外键）
    is_read    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_sys_notification PRIMARY KEY (id),
    CONSTRAINT fk_sys_notification_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE
);

CREATE INDEX idx_notification_user ON sys_notification (user_id, is_read, created_at);

COMMENT ON TABLE  sys_notification IS '站内通知';
COMMENT ON COLUMN sys_notification.user_id IS '接收人 → sys_user.id';
COMMENT ON COLUMN sys_notification.title   IS '标题';
COMMENT ON COLUMN sys_notification.content IS '内容';
COMMENT ON COLUMN sys_notification.type    IS '类型，默认 feedback';
COMMENT ON COLUMN sys_notification.ref_id  IS '关联业务 ID（多态）';
COMMENT ON COLUMN sys_notification.is_read IS '是否已读';

-- ---------------------------------------------------------------------
-- 数据分析模块（V5）
-- ---------------------------------------------------------------------

-- 查询日志（搜索/问答），仪表盘聚合源
CREATE TABLE stat_query_log (
    id         BIGINT      NOT NULL,
    user_id    BIGINT,
    query_type VARCHAR(8)  NOT NULL,              -- search / qa
    keyword    VARCHAR(255),
    has_result BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_stat_query_log PRIMARY KEY (id),
    CONSTRAINT fk_stat_query_log_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE SET NULL
);

CREATE INDEX idx_stat_time ON stat_query_log (created_at, query_type);

COMMENT ON TABLE  stat_query_log IS '查询日志（搜索/问答，仪表盘聚合源）';
COMMENT ON COLUMN stat_query_log.user_id    IS '用户 ID → sys_user.id（可空）';
COMMENT ON COLUMN stat_query_log.query_type IS '类型：search/qa';
COMMENT ON COLUMN stat_query_log.keyword    IS '关键词';
COMMENT ON COLUMN stat_query_log.has_result IS '是否有结果';

-- ---------------------------------------------------------------------
-- RAG 向量块 kb_chunk（可选，依赖 pgvector）
--   Spring AI PgVectorStore 标准列 + search_tsv 全文列。
--   kb_chunk 通过 metadata 逻辑关联 kb_knowledge（无数据库外键）。
--   嵌入式 PG / 无 pgvector 环境跳过本段（RAG 检索仅生产可用）。
-- ---------------------------------------------------------------------

DO $ai$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_available_extensions WHERE name = 'vector') THEN
        EXECUTE $ddl$CREATE EXTENSION IF NOT EXISTS vector$ddl$;

        EXECUTE $ddl$
        CREATE TABLE IF NOT EXISTS kb_chunk (
            id         VARCHAR(64) PRIMARY KEY,   -- 应用侧生成的块 ID
            content    TEXT,                      -- 分块文本
            metadata   JSON,                      -- 含 knowledgeId/versionNo/title 等
            embedding  vector(1024),              -- 向量（模型维度 1024）
            search_tsv tsvector                   -- 全文列，触发器维护
        )
        $ddl$;

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

        EXECUTE $ddl$
        CREATE INDEX IF NOT EXISTS idx_kb_chunk_tsv_gin ON kb_chunk USING GIN (search_tsv)
        $ddl$;

        EXECUTE $ddl$
        CREATE INDEX IF NOT EXISTS idx_kb_chunk_hnsw ON kb_chunk USING hnsw (embedding vector_cosine_ops)
        $ddl$;

        EXECUTE $ddl$
        COMMENT ON TABLE kb_chunk IS 'RAG 向量块（pgvector，逻辑关联 kb_knowledge）'
        $ddl$;
    END IF;
END
$ai$;
