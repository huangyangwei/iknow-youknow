package com.huangyangwei.iknow.module.auth.service;

import com.huangyangwei.iknow.api.contract.AuthApi;
import com.huangyangwei.iknow.api.dto.auth.LoginRequest;
import com.huangyangwei.iknow.api.dto.auth.LoginResponse;
import com.huangyangwei.iknow.api.dto.auth.UserInfo;
import com.huangyangwei.iknow.common.api.ResultCode;
import com.huangyangwei.iknow.common.constant.Constants;
import com.huangyangwei.iknow.common.exception.BusinessException;
import com.huangyangwei.iknow.module.auth.entity.SysUser;
import com.huangyangwei.iknow.module.auth.mapper.SysUserMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 认证服务：实现 iknow-api 的 AuthApi 契约。
 * login 按 provider 标识路由到具体 AuthProvider；me 返回当前用户资料与角色。
 */
@Service
public class AuthService implements AuthApi {

    private final List<AuthProvider> providers;
    private final SysUserMapper userMapper;
    private final RoleLoader roleLoader;

    public AuthService(List<AuthProvider> providers, SysUserMapper userMapper, RoleLoader roleLoader) {
        this.providers = providers;
        this.userMapper = userMapper;
        this.roleLoader = roleLoader;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        String providerName = request.getProvider() == null || request.getProvider().isBlank()
                ? Constants.DEFAULT_AUTH_PROVIDER
                : request.getProvider();
        AuthProvider provider = providers.stream()
                .filter(p -> p.getName().equals(providerName))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ResultCode.LOGIN_FAILED, "不支持的登录方式: " + providerName));
        return provider.login(request);
    }

    @Override
    public UserInfo me(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND, "用户不存在");
        }
        List<String> roles = roleLoader.roleCodes(userId);
        return new UserInfo(user.getId(), user.getUsername(), user.getEmail(),
                user.getNickname(), roles, user.getCreatedAt());
    }
}
