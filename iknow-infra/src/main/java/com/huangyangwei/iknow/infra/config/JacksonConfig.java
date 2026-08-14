package com.huangyangwei.iknow.infra.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson 3（tools.jackson）全局定制：
 * 序列化时忽略 null 字段，保证响应体精简。
 */
@Configuration
public class JacksonConfig {

    @Bean
    public JsonMapperBuilderCustomizer jsonMapperBuilderCustomizer() {
        return builder -> builder.changeDefaultPropertyInclusion(
                value -> value.withValueInclusion(JsonInclude.Include.NON_NULL));
    }
}
