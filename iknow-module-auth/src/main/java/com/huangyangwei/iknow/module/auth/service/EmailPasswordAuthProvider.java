package com.huangyangwei.iknow.module.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huangyangwei.iknow.api.dto.auth.LoginRequest;
import com.huangyangwei.iknow.api.dto.auth.LoginResponse;
import com.huangyangwei.iknow.api.dto.auth.UserInfo;
import com.huangyangwei.iknow.common.api.ResultCode;
import com.huangyangwei.iknow.common.constant.Constants;
import com.huangyangwei.iknow.common.exception.BusinessException;
import com.huangyangwei.iknow.common.util.JwtUtil;
import com.huangyangwei.iknow.module.auth.entity.SysUser;
import com.huangyangwei.iknow.module.auth.mapper.SysUserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 默认认证实现：邮箱 + 密码 → BCrypt 校验 → 签发 JWT（含角色与权限）。
 */
@Component
public class EmailPasswordAuthProvider implements AuthProvider {

    private final SysUserMapper userMapper;
    private final RoleLoader roleLoader;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public EmailPasswordAuthProvider(SysUserMapper userMapper, RoleLoader roleLoader,
                                     PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.roleLoader = roleLoader;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public String getName() {
        return Constants.DEFAULT_AUTH_PROVIDER;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getEmail, request.getEmail().trim().toLowerCase()));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.LOGIN_FAILED);
        }
        if (user.getStatus() != null && user.getStatus() != 1) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        }
        List<String> roles = roleLoader.roleCodes(user.getId());
        List<String> permissions = roleLoader.permissions(user.getId());
        String token = jwtUtil.generate(user.getId(), user.getUsername(), roles, permissions);
        UserInfo userInfo = new UserInfo(user.getId(), user.getUsername(), user.getEmail(),
                user.getNickname(), roles, user.getCreatedAt());
        return new LoginResponse(token, "Bearer", jwtUtil.getExpireSeconds(), userInfo);
    }
}
