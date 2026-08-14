package com.huangyangwei.iknow.infra.security;

import com.huangyangwei.iknow.common.constant.Constants;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置（iknow.jwt.*）：密钥长度须 ≥ 32 字节（HS256）。
 */
@Component
@ConfigurationProperties(prefix = "iknow.jwt")
public class JwtProperties {

    private String secret = "iknow-youknow-dev-secret-change-me-in-prod-0123456789";
    private long expireSeconds = 7200;
    private String header = Constants.HEADER_AUTHORIZATION;
    private String prefix = Constants.TOKEN_PREFIX;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpireSeconds() {
        return expireSeconds;
    }

    public void setExpireSeconds(long expireSeconds) {
        this.expireSeconds = expireSeconds;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
}
