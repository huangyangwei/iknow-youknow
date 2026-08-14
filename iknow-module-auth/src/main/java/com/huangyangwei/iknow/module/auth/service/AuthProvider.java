package com.huangyangwei.iknow.module.auth.service;

import com.huangyangwei.iknow.api.dto.auth.LoginRequest;
import com.huangyangwei.iknow.api.dto.auth.LoginResponse;

/**
 * 认证提供方抽象：email_password 为默认实现，SSO/OAuth 后续以新实现扩展。
 * AuthService 按请求中的 provider 标识路由到具体实现。
 */
public interface AuthProvider {

    /** 提供方标识，如 email_password / sso_oauth2 */
    String getName();

    LoginResponse login(LoginRequest request);
}
