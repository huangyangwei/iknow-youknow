package com.huangyangwei.iknow.common.security;

import com.huangyangwei.iknow.common.constant.Constants;
import io.jsonwebtoken.Claims;

import java.util.ArrayList;
import java.util.List;

/**
 * 当前登录用户主信息：从 JWT Claims 反序列化，由认证过滤器写入 Spring SecurityContext。
 * 角色/权限均以原始字符串存放；authorityStrings() 生成 Spring Security 权限集合
 * （角色带 ROLE_ 前缀，权限为原始串）。
 */
public record CurrentUser(Long id, String username, List<String> roles, List<String> permissions) {

    @SuppressWarnings("unchecked")
    public static CurrentUser fromClaims(Claims claims) {
        Long uid = Long.valueOf(claims.getSubject());
        String username = claims.get("username", String.class);
        List<String> roles = claims.get("roles", List.class);
        List<String> permissions = claims.get("permissions", List.class);
        return new CurrentUser(uid, username,
                roles == null ? List.of() : List.copyOf(roles),
                permissions == null ? List.of() : List.copyOf(permissions));
    }

    public List<String> authorityStrings() {
        List<String> authorities = new ArrayList<>();
        if (roles != null) {
            for (String role : roles) {
                authorities.add(Constants.ROLE_PREFIX + role);
            }
        }
        if (permissions != null) {
            authorities.addAll(permissions);
        }
        return authorities;
    }
}
