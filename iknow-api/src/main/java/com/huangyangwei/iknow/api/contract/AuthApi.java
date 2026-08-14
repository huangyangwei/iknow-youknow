package com.huangyangwei.iknow.api.contract;

import com.huangyangwei.iknow.api.dto.auth.LoginRequest;
import com.huangyangwei.iknow.api.dto.auth.LoginResponse;
import com.huangyangwei.iknow.api.dto.auth.UserInfo;

/**
 * 认证模块对外契约（iknow-api 层）。
 * 多模块单体阶段由 iknow-module-auth 的 AuthService 实现；
 * 未来拆微服务时本接口转为 Feign 客户端契约，调用方无需感知实现迁移。
 */
public interface AuthApi {

    LoginResponse login(LoginRequest request);

    UserInfo me(Long userId);
}
