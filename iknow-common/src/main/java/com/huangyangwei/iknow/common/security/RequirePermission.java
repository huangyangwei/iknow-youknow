package com.huangyangwei.iknow.common.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * RBAC 权限注解：标注在方法上，要求当前登录用户拥有指定权限点。
 * 由 iknow-infra 的 PermissionAspect 执行鉴权，权限不足返回 2003 FORBIDDEN。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {

    /** 权限点标识，如 knowledge:create */
    String value();
}
