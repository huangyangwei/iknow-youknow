package com.huangyangwei.iknow.common.constant;

/**
 * 全局常量：认证头、角色/权限前缀、缓存名、TraceId 键。
 */
public final class Constants {

    private Constants() {
    }

    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    public static final String ROLE_PREFIX = "ROLE_";
    public static final String PERMISSION_PREFIX = "PERM_";

    public static final String TRACE_ID = "traceId";

    public static final String CACHE_USER = "user";
    public static final String CACHE_ROLE = "role";
    public static final String CACHE_KNOWLEDGE_DETAIL = "knowledge:detail";
    public static final String CACHE_SYSTEM_CONFIG = "system:config";

    // 内置角色码
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_EDITOR = "EDITOR";
    public static final String ROLE_MEMBER = "MEMBER";

    // 权限点（RBAC 注解值）
    public static final String PERM_KNOWLEDGE_CREATE = "knowledge:create";
    public static final String PERM_KNOWLEDGE_UPDATE = "knowledge:update";
    public static final String PERM_KNOWLEDGE_DELETE = "knowledge:delete";
    public static final String PERM_USER_MANAGE = "user:manage";
    public static final String PERM_FEEDBACK_HANDLE = "feedback:handle";

    public static final String DEFAULT_AUTH_PROVIDER = "email_password";
}
