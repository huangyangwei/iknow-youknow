package com.huangyangwei.iknow.infra.security;

import com.huangyangwei.iknow.common.util.JwtUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JWT 工具 Bean：由 iknow.jwt.* 配置构建。
 */
@Configuration
public class JwtConfig {

    @Bean
    public JwtUtil jwtUtil(JwtProperties properties) {
        return new JwtUtil(properties.getSecret(), properties.getExpireSeconds());
    }
}
