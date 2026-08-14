package com.huangyangwei.iknow.common.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * RBAC 角色注解：标注在方法上，要求当前登录用户拥有指定角色。
 * 由 iknow-infra 的 PermissionAspect 执行鉴权。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {

    /** 角色码，如 ADMIN */
    String value();
}
