-- 认证模块初始表结构 + 种子数据（管理员、角色、权限）

CREATE TABLE sys_user (
    id          BIGINT       NOT NULL,
    username    VARCHAR(64)  NOT NULL,
    email       VARCHAR(128) NOT NULL,
    password    VARCHAR(128) NOT NULL,
    nickname    VARCHAR(64),
    avatar      VARCHAR(255),
    status      SMALLINT     NOT NULL DEFAULT 1,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_sys_user PRIMARY KEY (id),
    CONSTRAINT uk_sys_user_username UNIQUE (username),
    CONSTRAINT uk_sys_user_email UNIQUE (email)
);

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

CREATE TABLE sys_user_role (
    id         BIGINT    NOT NULL,
    user_id    BIGINT    NOT NULL,
    role_id    BIGINT    NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_sys_user_role PRIMARY KEY (id),
    CONSTRAINT uk_sys_user_role UNIQUE (user_id, role_id)
);

CREATE TABLE sys_role_permission (
    id         BIGINT      NOT NULL,
    role_id    BIGINT      NOT NULL,
    permission VARCHAR(64) NOT NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_sys_role_permission PRIMARY KEY (id),
    CONSTRAINT uk_sys_role_permission UNIQUE (role_id, permission)
);

-- 内置角色
INSERT INTO sys_role (id, code, name, description, created_at, updated_at) VALUES
    (1, 'ADMIN',  '管理员', '系统管理员，拥有全部权限', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'EDITOR', '编辑',   '可创建/更新知识库内容',   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 'MEMBER', '成员',   '普通成员，可浏览知识库',   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ADMIN 角色权限点
INSERT INTO sys_role_permission (id, role_id, permission, created_at) VALUES
    (1,  1, 'knowledge:create', CURRENT_TIMESTAMP),
    (2,  1, 'knowledge:update', CURRENT_TIMESTAMP),
    (3,  1, 'knowledge:delete', CURRENT_TIMESTAMP),
    (4,  1, 'user:manage',      CURRENT_TIMESTAMP),
    (5,  1, 'feedback:handle',  CURRENT_TIMESTAMP);

-- EDITOR 角色权限点
INSERT INTO sys_role_permission (id, role_id, permission, created_at) VALUES
    (6,  2, 'knowledge:create', CURRENT_TIMESTAMP),
    (7,  2, 'knowledge:update', CURRENT_TIMESTAMP);

-- 默认管理员：admin@iknow.ai / Admin@123（BCrypt）
INSERT INTO sys_user (id, username, email, password, nickname, status, created_at, updated_at) VALUES
    (1, 'admin', 'admin@iknow.ai',
     '$2a$10$YI.//Pd68ZzF3EYJzhK.SeyvuVrGob7eCF33cItTH.PjSFY.zh7UK',
     '系统管理员', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO sys_user_role (id, user_id, role_id, created_at) VALUES
    (1, 1, 1, CURRENT_TIMESTAMP);
