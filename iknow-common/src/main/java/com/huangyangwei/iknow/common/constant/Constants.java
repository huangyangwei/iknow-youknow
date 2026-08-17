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
    public static final String CACHE_CATEGORY_TREE = "knowledge:category";
    public static final String CACHE_TAG_DICT = "knowledge:tag";
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

    // 知识状态
    public static final String KNOWLEDGE_STATUS_DRAFT = "draft";
    public static final String KNOWLEDGE_STATUS_PUBLISHED = "published";
    public static final String KNOWLEDGE_STATUS_ARCHIVED = "archived";
    public static final String KNOWLEDGE_STATUS_PENDING_PUBLISH = "pending_publish";

    // 反馈类型与状态（P3，技术方案 §9.2 fb_feedback）
    public static final String FEEDBACK_TYPE_LIKE = "like";
    public static final String FEEDBACK_TYPE_DISLIKE = "dislike";
    public static final String FEEDBACK_TYPE_CORRECTION = "correction";
    public static final String FEEDBACK_TYPE_SUGGESTION = "suggestion";
    public static final String FEEDBACK_STATUS_PENDING = "pending";
    public static final String FEEDBACK_STATUS_PROCESSING = "processing";
    public static final String FEEDBACK_STATUS_RESOLVED = "resolved";

    // 站内通知
    public static final String NOTIFICATION_TYPE_FEEDBACK = "feedback";

    // 统计查询类型（P3，技术方案 §9.2 stat_query_log）
    public static final String QUERY_TYPE_SEARCH = "search";
    public static final String QUERY_TYPE_QA = "qa";
}
