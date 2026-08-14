package com.huangyangwei.iknow.spike1.server;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Spike ① 测试配置：启动真实 PostgreSQL 16（embedded-postgres，Windows amd64 二进制），
 * 执行 schema-pg.sql 建表，并提供 DataSource。
 * 用于在无独立 PG 实例的沙箱中验证 MyBatis-Plus + PostgreSQL 真实联通。
 */
@TestConfiguration
public class EmbeddedPostgresConfig {

    private static final String SCHEMA_SQL = "/db/schema-pg.sql";

    @Bean(destroyMethod = "close")
    public EmbeddedPostgres embeddedPostgres() throws IOException {
        return EmbeddedPostgres.builder()
                .setPort(0)
                .start();
    }

    @Bean
    public DataSource dataSource(EmbeddedPostgres embeddedPostgres) throws Exception {
        DataSource ds = embeddedPostgres.getPostgresDatabase();
        try (Connection conn = ds.getConnection();
             Statement st = conn.createStatement();
             InputStream in = EmbeddedPostgresConfig.class.getResourceAsStream(SCHEMA_SQL)) {
            if (in == null) {
                throw new IllegalStateException("schema resource not found: " + SCHEMA_SQL);
            }
            st.execute(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
        return ds;
    }
}
