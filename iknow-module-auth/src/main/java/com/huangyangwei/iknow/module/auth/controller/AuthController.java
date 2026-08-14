package com.huangyangwei.iknow.module.auth.controller;

import com.huangyangwei.iknow.api.dto.auth.LoginRequest;
import com.huangyangwei.iknow.api.dto.auth.LoginResponse;
import com.huangyangwei.iknow.api.dto.auth.UserInfo;
import com.huangyangwei.iknow.common.api.Result;
import com.huangyangwei.iknow.common.api.ResultCode;
import com.huangyangwei.iknow.common.constant.Constants;
import com.huangyangwei.iknow.common.exception.BusinessException;
import com.huangyangwei.iknow.common.security.CurrentUser;
import com.huangyangwei.iknow.common.security.RequirePermission;
import com.huangyangwei.iknow.common.security.RequireRole;
import com.huangyangwei.iknow.module.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 认证接口：登录、当前用户、RBAC 鉴权演示。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    @GetMapping("/me")
    public Result<UserInfo> me() {
        return Result.ok(authService.me(currentUser().id()));
    }

    @GetMapping("/roles")
    public Result<List<String>> roles() {
        return Result.ok(currentUser().roles());
    }

    @GetMapping("/admin/ping")
    @RequireRole(Constants.ROLE_ADMIN)
    @RequirePermission(Constants.PERM_USER_MANAGE)
    public Result<String> adminPing() {
        return Result.ok("admin:ok");
    }

    private CurrentUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CurrentUser currentUser) {
            return currentUser;
        }
        throw new BusinessException(ResultCode.UNAUTHORIZED);
    }
}
