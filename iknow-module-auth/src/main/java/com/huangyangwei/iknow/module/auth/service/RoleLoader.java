package com.huangyangwei.iknow.module.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huangyangwei.iknow.common.constant.Constants;
import com.huangyangwei.iknow.module.auth.entity.SysRole;
import com.huangyangwei.iknow.module.auth.entity.SysRolePermission;
import com.huangyangwei.iknow.module.auth.entity.SysUserRole;
import com.huangyangwei.iknow.module.auth.mapper.SysRoleMapper;
import com.huangyangwei.iknow.module.auth.mapper.SysRolePermissionMapper;
import com.huangyangwei.iknow.module.auth.mapper.SysUserRoleMapper;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 角色/权限加载：按用户 ID 查询其角色码与权限点，供登录签发 JWT 与 /auth/me 使用。
 * 角色码经两级缓存（L1 Caffeine + L2 Redis）缓存，Redis 不可用时自动降级仅 L1。
 */
@Component
public class RoleLoader {

    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;

    public RoleLoader(SysUserRoleMapper userRoleMapper, SysRoleMapper roleMapper,
                      SysRolePermissionMapper rolePermissionMapper) {
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.rolePermissionMapper = rolePermissionMapper;
    }

    @Cacheable(value = Constants.CACHE_ROLE, key = "#userId")
    public List<String> roleCodes(Long userId) {
        List<Long> roleIds = roleIdsOf(userId);
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return roleMapper.selectBatchIds(roleIds).stream()
                .map(SysRole::getCode)
                .toList();
    }

    public List<String> permissions(Long userId) {
        List<Long> roleIds = roleIdsOf(userId);
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return rolePermissionMapper.selectList(new LambdaQueryWrapper<SysRolePermission>()
                        .in(SysRolePermission::getRoleId, roleIds))
                .stream()
                .map(SysRolePermission::getPermission)
                .distinct()
                .toList();
    }

    private List<Long> roleIdsOf(Long userId) {
        return userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, userId))
                .stream()
                .map(SysUserRole::getRoleId)
                .toList();
    }
}
