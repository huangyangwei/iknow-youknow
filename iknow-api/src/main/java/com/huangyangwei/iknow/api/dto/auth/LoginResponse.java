package com.huangyangwei.iknow.api.dto.auth;

import java.io.Serializable;

/**
 * 登录响应：JWT 访问令牌 + 令牌类型 + 有效期 + 用户信息。
 */
public class LoginResponse implements Serializable {

    private String accessToken;
    private String tokenType;
    private long expiresIn;
    private UserInfo user;

    public LoginResponse() {
    }

    public LoginResponse(String accessToken, String tokenType, long expiresIn, UserInfo user) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.user = user;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public UserInfo getUser() {
        return user;
    }

    public void setUser(UserInfo user) {
        this.user = user;
    }
}
