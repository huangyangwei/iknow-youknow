package com.huangyangwei.iknow.config;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.io.IOException;

/**
 * dev 环境数据源：嵌入式 PostgreSQL 16（zonky），无需外部数据库即可启动骨架。
 * 生产环境使用 spring.profiles.active=prod，此时本配置不生效，改走 application.yml 外部数据源。
 */
@Configuration
@Profile("dev")
public class EmbeddedPostgresConfig {

    @Bean(destroyMethod = "close")
    public EmbeddedPostgres embeddedPostgres() throws IOException {
        return EmbeddedPostgres.builder().setPort(0).start();
    }

    @Bean
    @Primary
    public DataSource dataSource(EmbeddedPostgres embeddedPostgres) throws Exception {
        return embeddedPostgres.getPostgresDatabase();
    }
}
