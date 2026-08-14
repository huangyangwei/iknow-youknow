package com.huangyangwei.iknow.infra.audit;

import com.huangyangwei.iknow.common.security.CurrentUser;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 审计切面：记录 actor/action/resource/success/耗时。当前以结构化日志输出，
 * 预留落库扩展点。
 */
@Aspect
@Component
public class AuditLogAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditLogAspect.class);

    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        long start = System.currentTimeMillis();
        String actor = resolveActor();
        boolean success = true;
        String error = null;
        try {
            return joinPoint.proceed();
        } catch (Throwable t) {
            success = false;
            error = t.getMessage();
            throw t;
        } finally {
            long cost = System.currentTimeMillis() - start;
            log.info("audit | actor={} | action={} | resource={} | success={} | cost={}ms | error={}",
                    actor, auditLog.action(), auditLog.resource(), success, cost, error);
        }
    }

    private String resolveActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CurrentUser currentUser) {
            return currentUser.username();
        }
        return "anonymous";
    }
}
