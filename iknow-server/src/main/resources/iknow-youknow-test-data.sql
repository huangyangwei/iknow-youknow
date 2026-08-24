-- =====================================================================
-- iknow-youknow 企业知识库问答系统 —— 测试数据（INSERT）
--
-- 前置：先执行 iknow-youknow-schema.sql 建表。
-- 说明：
--   1. 主键 id 使用小整数以便外键引用自洽（与 MyBatis-Plus 雪花 ID 兼容，
--      测试库中显式指定即可）。
--   2. 覆盖 14 张常驻表 + 1 张可选表（kb_chunk），外键引用顺序已按依赖排列：
--      角色/权限 → 用户 → 用户-角色 → 分类/标签 → 知识 → 版本/知识-标签
--      → 会话/消息 → 反馈/通知 → 查询日志 →（可选）向量块。
--   3. 所有测试账号密码均为 Admin@123（BCrypt 哈希取自 V1 种子，可直接登录）。
--   4. 数据规模：用户 4、角色 3、权限 7、分类 4、标签 5、知识 5、版本 6、
--      知识-标签 7、会话 3、消息 6、反馈 4、通知 2、查询日志 8、（可选）向量块 1。
-- =====================================================================

-- ---------------------------------------------------------------------
-- 认证模块
-- ---------------------------------------------------------------------

-- 角色
INSERT INTO sys_role (id, code, name, description) VALUES
    (1, 'ADMIN',  '管理员', '系统管理员，拥有全部权限'),
    (2, 'EDITOR', '编辑',   '可创建/更新知识库内容'),
    (3, 'MEMBER', '成员',   '普通成员，可浏览知识库');

-- 角色-权限
INSERT INTO sys_role_permission (id, role_id, permission) VALUES
    (1, 1, 'knowledge:create'),
    (2, 1, 'knowledge:update'),
    (3, 1, 'knowledge:delete'),
    (4, 1, 'user:manage'),
    (5, 1, 'feedback:handle'),
    (6, 2, 'knowledge:create'),
    (7, 2, 'knowledge:update');

-- 用户（密码统一为 Admin@123 的 BCrypt 哈希）
INSERT INTO sys_user (id, username, email, password, nickname, avatar, status) VALUES
    (1, 'admin',   'admin@iknow.ai',   '$2a$10$YI.//Pd68ZzF3EYJzhK.SeyvuVrGob7eCF33cItTH.PjSFY.zh7UK', '系统管理员', NULL, 1),
    (2, 'editor1', 'editor1@iknow.ai', '$2a$10$YI.//Pd68ZzF3EYJzhK.SeyvuVrGob7eCF33cItTH.PjSFY.zh7UK', '内容编辑甲', NULL, 1),
    (3, 'member1', 'member1@iknow.ai', '$2a$10$YI.//Pd68ZzF3EYJzhK.SeyvuVrGob7eCF33cItTH.PjSFY.zh7UK', '普通成员甲', NULL, 1),
    (4, 'member2', 'member2@iknow.ai', '$2a$10$YI.//Pd68ZzF3EYJzhK.SeyvuVrGob7eCF33cItTH.PjSFY.zh7UK', '普通成员乙', NULL, 1);

-- 用户-角色
INSERT INTO sys_user_role (id, user_id, role_id) VALUES
    (1, 1, 1),   -- admin  → ADMIN
    (2, 2, 2),   -- editor1 → EDITOR
    (3, 3, 3),   -- member1 → MEMBER
    (4, 4, 3);   -- member2 → MEMBER

-- ---------------------------------------------------------------------
-- 知识管理模块
-- ---------------------------------------------------------------------

-- 分类（树形：parent_id=0 为一级）
INSERT INTO kb_category (id, parent_id, name, product_line, sort, level, path) VALUES
    (1, 0, '产品中心', '全线产品', 1, 1, '/1'),
    (2, 1, '技术文档', '全线产品', 1, 2, '/1/2'),
    (3, 1, '常见问题', '全线产品', 2, 2, '/1/3'),
    (4, 0, '人事制度', NULL,      2, 1, '/4');

-- 标签
INSERT INTO kb_tag (id, name) VALUES
    (1, '入门'), (2, '高级'), (3, '账号'), (4, '性能'), (5, '安全');

-- 知识条目（覆盖 draft/published/archived/pending_publish 各状态）
INSERT INTO kb_knowledge
    (id, title, html_content, plain_text, summary, category_id, knowledge_type, status,
     version_no, publish_time, scheduled_publish_time, view_count, like_count, created_by, updated_by)
VALUES
    (101, '如何修改登录密码',
     '<p>进入「个人中心 → 账号安全 → 修改密码」，输入原密码与新密码即可。</p>',
     '进入个人中心，账号安全，修改密码即可。',
     '修改登录密码的操作步骤', 3, 'FAQ', 'published', 1,
     '2026-08-01 09:00:00', NULL, 128, 12, 2, 2),
    (102, '系统部署指南',
     '<h2>环境要求</h2><p>JDK 21、Maven 3.9、PostgreSQL 16。</p><h2>步骤</h2><p>构建 → 建表 → 启动。</p>',
     '环境要求 JDK 21 Maven 3.9 PostgreSQL 16。步骤：构建、建表、启动。',
     '生产环境部署步骤', 2, 'DOC', 'published', 2,
     '2026-08-10 10:30:00', NULL, 512, 45, 2, 2),
    (103, '新员工入职指引',
     '<p>入职第一天办理工牌、开通账号，参加新人培训。</p>',
     '入职第一天办理工牌开通账号参加新人培训。',
     '新员工入职流程', 4, 'FAQ', 'draft', 1,
     NULL, NULL, 0, 0, 2, 2),
    (104, '性能调优最佳实践',
     '<p>JVM 参数、连接池、缓存策略与慢查询优化。</p>',
     'JVM 参数连接池缓存策略慢查询优化。',
     '性能调优要点', 2, 'DOC', 'pending_publish', 1,
     NULL, '2026-08-25 08:00:00', 0, 0, 2, 2),
    (105, '数据备份与恢复方案',
     '<p>每日全量 + 增量备份，恢复时先全量后增量。</p>',
     '每日全量加增量备份，恢复时先全量后增量。',
     '备份恢复方案', 2, 'DOC', 'archived', 1,
     '2026-05-20 14:00:00', NULL, 89, 6, 2, 2);

-- 版本快照
INSERT INTO kb_knowledge_version
    (id, knowledge_id, version_no, title, html_content, plain_text, summary, change_note, created_by)
VALUES
    (201, 101, 1, '如何修改登录密码',
     '<p>进入「个人中心 → 账号安全 → 修改密码」。</p>',
     '进入个人中心，账号安全，修改密码即可。', '修改登录密码的操作步骤', '首次发布', 2),
    (202, 102, 1, '系统部署指南',
     '<h2>环境要求</h2><p>JDK 21、Maven、PostgreSQL 16。</p>',
     '环境要求 JDK 21 Maven PostgreSQL 16。', '生产环境部署步骤', '首次发布', 2),
    (203, 102, 2, '系统部署指南',
     '<h2>环境要求</h2><p>JDK 21、Maven 3.9、PostgreSQL 16。</p><h2>步骤</h2><p>构建 → 建表 → 启动。</p>',
     '环境要求 JDK 21 Maven 3.9 PostgreSQL 16。步骤：构建、建表、启动。', '生产环境部署步骤', '补充容器化部署', 2),
    (204, 103, 1, '新员工入职指引',
     '<p>入职第一天办理工牌、开通账号。</p>',
     '入职第一天办理工牌开通账号。', '新员工入职流程', '创建草稿', 2),
    (205, 104, 1, '性能调优最佳实践',
     '<p>JVM 参数、连接池、缓存策略。</p>',
     'JVM 参数连接池缓存策略。', '性能调优要点', '创建草稿', 2),
    (206, 105, 1, '数据备份与恢复方案',
     '<p>每日全量 + 增量备份。</p>',
     '每日全量加增量备份。', '备份恢复方案', '首次发布', 2);

-- 知识-标签关联
INSERT INTO kb_knowledge_tag (id, knowledge_id, tag_id) VALUES
    (301, 101, 3), (302, 101, 1),
    (303, 102, 2), (304, 102, 4),
    (305, 103, 1),
    (306, 104, 4),
    (307, 105, 5);

-- ---------------------------------------------------------------------
-- AI 模块
-- ---------------------------------------------------------------------

-- 问答会话
INSERT INTO qa_session (id, user_id, title) VALUES
    (1001, 3, '如何修改登录密码'),
    (1002, 3, '系统部署相关'),
    (1003, 4, '接口性能问题');

-- 问答消息（sources 为引用 JSON 数组文本）
INSERT INTO qa_message (id, session_id, role, content, model, confidence, sources, tokens) VALUES
    (2001, 1001, 'user',      '如何修改登录密码？',
     NULL, NULL, NULL, NULL),
    (2002, 1001, 'assistant', '进入「个人中心 → 账号安全 → 修改密码」，输入原密码和新密码即可。',
     'gpt-4o-mini', 'high',
     '[{"title":"如何修改登录密码","knowledgeId":101,"versionNo":1,"score":0.92}]', 156),
    (2003, 1002, 'user',      '生产环境如何部署？',
     NULL, NULL, NULL, NULL),
    (2004, 1002, 'assistant', '请参考《系统部署指南》，需要 JDK 21、Maven 3.9 与 PostgreSQL 16。',
     'gpt-4o-mini', 'medium',
     '[{"title":"系统部署指南","knowledgeId":102,"versionNo":2,"score":0.78}]', 201),
    (2005, 1003, 'user',      '为什么接口响应很慢？',
     NULL, NULL, NULL, NULL),
    (2006, 1003, 'assistant', '抱歉，我暂时没有找到足够的信息回答该问题，建议提交反馈。',
     'gpt-4o-mini', 'low', '[]', 98);

-- ---------------------------------------------------------------------
-- 反馈闭环模块
-- ---------------------------------------------------------------------

-- 反馈（覆盖 like/dislike/correction/suggestion 与 pending/processing/resolved）
INSERT INTO fb_feedback
    (id, type, source_type, source_id, session_id, question, content, status,
     handler_id, handle_note, handled_at, created_by)
VALUES
    (3001, 'like',       'knowledge', 101,  1001, NULL,               '内容准确，很有帮助。',
     'resolved', 2, '感谢反馈，已确认。', '2026-08-12 10:00:00', 3),
    (3002, 'dislike',    'knowledge', 102,  NULL, NULL,               '部署步骤与当前版本不一致。',
     'pending',  NULL, NULL, NULL, 4),
    (3003, 'correction', 'knowledge', 102,  NULL, NULL,               '第 3 步命令缺少参数。',
     'processing', 2, NULL, NULL, 3),
    (3004, 'suggestion', 'qa',        NULL, 1003, '接口响应很慢',     '希望补充性能排查相关文档。',
     'resolved', 2, '已新增《性能调优最佳实践》。', '2026-08-20 15:30:00', 4);

-- 站内通知
INSERT INTO sys_notification (id, user_id, title, content, type, ref_id, is_read) VALUES
    (4001, 3, '您的反馈已处理', '您对《如何修改登录密码》的点赞反馈已被处理。', 'feedback', 3001, TRUE),
    (4002, 4, '您的建议已采纳', '您关于性能排查的建议已采纳，请查看新增文档。', 'feedback', 3004, FALSE);

-- ---------------------------------------------------------------------
-- 数据分析模块
-- ---------------------------------------------------------------------

-- 查询日志（覆盖 search/qa 与有/无结果，时间序列供仪表盘聚合）
INSERT INTO stat_query_log (id, user_id, query_type, keyword, has_result, created_at) VALUES
    (5001, 3,    'search', '修改密码',       TRUE,  '2026-08-21 09:00:00'),
    (5002, 3,    'search', '部署',           TRUE,  '2026-08-21 09:05:00'),
    (5003, 4,    'search', '性能',           FALSE, '2026-08-21 10:00:00'),
    (5004, 3,    'qa',     '如何修改登录密码', TRUE,  '2026-08-22 14:00:00'),
    (5005, 4,    'qa',     '接口性能问题',   FALSE, '2026-08-22 15:00:00'),
    (5006, NULL, 'search', '备份',           FALSE, '2026-08-23 11:00:00'),
    (5007, 3,    'search', '修改密码',       TRUE,  '2026-08-23 16:00:00'),
    (5008, 4,    'qa',     '部署',           TRUE,  '2026-08-24 09:00:00');

-- ---------------------------------------------------------------------
-- 可选：RAG 向量块 kb_chunk（仅当 pgvector 已启用、kb_chunk 表存在时生效）
--   说明：embedding 需要真实 1024 维向量，此处以 NULL 占位；生产环境由
--         ChunkVectorizationService 在知识发布后写入真实向量。
-- ---------------------------------------------------------------------
DO $chunk$
BEGIN
    IF to_regclass('kb_chunk') IS NOT NULL THEN
        INSERT INTO kb_chunk (id, content, metadata, embedding) VALUES
            ('chunk-101-1', '进入个人中心，账号安全，修改密码即可。',
             '{"knowledgeId":101,"versionNo":1,"title":"如何修改登录密码"}'::json, NULL);
    END IF;
END
$chunk$;
