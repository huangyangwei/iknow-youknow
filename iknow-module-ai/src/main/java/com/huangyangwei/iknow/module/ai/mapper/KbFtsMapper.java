package com.huangyangwei.iknow.module.ai.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * PG 全文检索（kb_knowledge.search_tsv GIN 索引），仅返回 published 条目。
 * 分词配置由 kb_fts_query 与 V2 触发函数同一解析（iknow_zhcfg 优先，回退 simple）。
 */
public interface KbFtsMapper {

    @Select("SELECT id, version_no AS version_no, title, summary, plain_text AS plain_text, "
            + "ts_rank_cd(search_tsv, kb_fts_query(#{keyword})) AS rank "
            + "FROM kb_knowledge "
            + "WHERE status = 'published' AND search_tsv @@ kb_fts_query(#{keyword}) "
            + "ORDER BY rank DESC LIMIT #{limit}")
    List<FtsHit> searchPublished(@Param("keyword") String keyword, @Param("limit") int limit);
}
