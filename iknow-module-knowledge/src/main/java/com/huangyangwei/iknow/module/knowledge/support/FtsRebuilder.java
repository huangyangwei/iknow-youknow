package com.huangyangwei.iknow.module.knowledge.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 全文索引重建触发器：发布/回滚后重建 kb_knowledge 的 GIN 索引。
 * search_tsv 由表触发器维护，这里做 REINDEX + ANALYZE 保证索引与统计信息最新。
 */
@Component
public class FtsRebuilder {

    private static final Logger log = LoggerFactory.getLogger(FtsRebuilder.class);

    private final JdbcTemplate jdbcTemplate;

    public FtsRebuilder(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void rebuild() {
        try {
            jdbcTemplate.execute("REINDEX TABLE kb_knowledge");
            jdbcTemplate.execute("ANALYZE kb_knowledge");
        } catch (Exception e) {
            // 索引重建失败不阻断业务（FTS 失效仅影响搜索，LIKE 检索通道不受影响）
            log.warn("fts rebuild failed", e);
        }
    }
}
