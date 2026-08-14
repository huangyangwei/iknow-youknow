package com.huangyangwei.iknow.infra.security;

import com.huangyangwei.iknow.common.api.ResultCode;
import com.huangyangwei.iknow.common.exception.BusinessException;
import com.huangyangwei.iknow.common.security.CurrentUser;
import com.huangyangwei.iknow.common.security.RequirePermission;
import com.huangyangwei.iknow.common.security.RequireRole;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * RBAC 鉴权切面：拦截 @RequirePermission / @RequireRole。
 * 未登录抛 2001 UNAUTHORIZED，权限/角色不足抛 2003 FORBIDDEN，统一由全局异常处理返回。
 */
@Aspect
@Component
public class PermissionAspect {

    @Around("@annotation(requirePermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint, RequirePermission requirePermission) throws Throwable {
        CurrentUser user = currentUser();
        if (user.permissions() == null || !user.permissions().contains(requirePermission.value())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        return joinPoint.proceed();
    }

    @Around("@annotation(requireRole)")
    public Object checkRole(ProceedingJoinPoint joinPoint, RequireRole requireRole) throws Throwable {
        CurrentUser user = currentUser();
        if (user.roles() == null || !user.roles().contains(requireRole.value())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        return joinPoint.proceed();
    }

    private CurrentUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CurrentUser currentUser) {
            return currentUser;
        }
        throw new BusinessException(ResultCode.UNAUTHORIZED);
    }
}
