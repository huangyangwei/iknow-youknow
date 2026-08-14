package com.huangyangwei.iknow.module.knowledge.support;

import com.huangyangwei.iknow.common.api.ResultCode;
import com.huangyangwei.iknow.common.exception.BusinessException;
import com.huangyangwei.iknow.common.security.CurrentUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 从 Spring SecurityContext 读取当前登录用户；未登录抛 2001。
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static CurrentUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CurrentUser currentUser) {
            return currentUser;
        }
        throw new BusinessException(ResultCode.UNAUTHORIZED);
    }
}
