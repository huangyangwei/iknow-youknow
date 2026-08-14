package com.huangyangwei.iknow.common.api;

/**
 * 统一错误码。
 * 分段：0 成功；1xxx 通用参数/系统；2xxx 权限认证；3xxx 知识域；4xxx 搜索/问答；5xxx 反馈。
 */
public enum ResultCode {

    SUCCESS(0, "ok"),

    // 1xxx 通用
    BAD_REQUEST(1000, "请求参数错误"),
    PARAM_VALIDATION_FAILED(1001, "参数校验失败"),
    RESOURCE_NOT_FOUND(1004, "资源不存在"),
    TOO_MANY_REQUESTS(1005, "请求过于频繁"),
    INTERNAL_ERROR(1500, "系统内部错误"),

    // 2xxx 权限认证
    UNAUTHORIZED(2001, "未登录或登录已过期"),
    FORBIDDEN(2003, "无权限访问"),
    LOGIN_FAILED(2004, "用户名或密码错误"),
    ACCOUNT_DISABLED(2005, "账号已被禁用"),
    TOKEN_INVALID(2006, "令牌无效或已过期"),

    // 3xxx 知识域
    KNOWLEDGE_NOT_FOUND(3000, "知识条目不存在"),
    KNOWLEDGE_OPERATION_FORBIDDEN(3001, "该操作不允许"),

    // 4xxx 搜索/问答
    SEARCH_ERROR(4000, "搜索服务异常"),
    QA_ERROR(4001, "问答服务异常"),

    // 5xxx 反馈
    FEEDBACK_ERROR(5000, "反馈服务异常"),

    UNKNOWN(9999, "未知错误");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
