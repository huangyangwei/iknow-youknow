package com.huangyangwei.iknow.module.ai.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Chunk-level PG full-text search over kb_chunk.search_tsv, restricted to published knowledge.
 */
public interface KbChunkFtsMapper {

    @Select("SELECT c.id, "
            + "(c.metadata->>'knowledgeId')::bigint AS knowledge_id, "
            + "COALESCE((c.metadata->>'versionNo')::int, k.version_no) AS version_no, "
            + "(c.metadata->>'chunkIndex')::int AS chunk_index, "
            + "COALESCE(NULLIF(c.metadata->>'title', ''), k.title) AS title, "
            + "c.content, "
            + "ts_rank_cd(c.search_tsv, kb_fts_query(#{keyword})) AS rank "
            + "FROM kb_chunk c "
            + "JOIN kb_knowledge k ON k.id = (c.metadata->>'knowledgeId')::bigint "
            + "WHERE k.status = 'published' "
            + "AND c.search_tsv @@ kb_fts_query(#{keyword}) "
            + "ORDER BY rank DESC LIMIT #{limit}")
    List<KbChunkFtsHit> searchPublishedChunks(@Param("keyword") String keyword, @Param("limit") int limit);
}
