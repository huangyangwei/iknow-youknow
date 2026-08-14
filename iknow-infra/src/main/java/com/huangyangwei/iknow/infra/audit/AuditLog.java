package com.huangyangwei.iknow.infra.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作审计注解：标注敏感操作，由 AuditLogAspect 记录操作人/动作/资源/结果。
 * 后续可扩展为写入 sys_operation_log。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {

    /** 操作动作，如 knowledge.publish */
    String action();

    /** 资源类型，如 knowledge */
    String resource() default "";
}
