package com.huangyangwei.iknow.module.analytics.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 全文检索（kb_knowledge.search_tsv GIN 索引，仅 published）。
 * 分词配置与 V2/V3 同一解析（iknow_zhcfg 优先，回退 simple）；rank 由 ts_rank_cd 计算。
 */
public interface SearchMapper {

    @Select("<script>"
            + "SELECT k.id AS id, k.title AS title, k.summary AS summary, "
            + "k.category_id AS categoryId, c.name AS categoryName, "
            + "k.knowledge_type AS knowledgeType, k.publish_time AS publishTime, "
            + "k.view_count AS viewCount, k.like_count AS likeCount, "
            + "ts_rank_cd(k.search_tsv, kb_fts_query(#{keyword})) AS rank "
            + "FROM kb_knowledge k "
            + "LEFT JOIN kb_category c ON c.id = k.category_id "
            + "WHERE k.status = 'published' AND k.search_tsv @@ kb_fts_query(#{keyword}) "
            + "<if test='categoryId != null'>AND k.category_id = #{categoryId}</if> "
            + "<if test='tagId != null'>AND EXISTS (SELECT 1 FROM kb_knowledge_tag kt "
            + "WHERE kt.knowledge_id = k.id AND kt.tag_id = #{tagId})</if> "
            + "<if test='timeFrom != null'>AND k.publish_time &gt;= #{timeFrom}</if> "
            + "<if test='timeTo != null'>AND k.publish_time &lt;= #{timeTo}</if> "
            + "ORDER BY rank DESC, k.id DESC "
            + "LIMIT #{size} OFFSET #{offset}"
            + "</script>")
    List<SearchRow> search(@Param("keyword") String keyword, @Param("categoryId") Long categoryId,
                           @Param("tagId") Long tagId, @Param("timeFrom") LocalDateTime timeFrom,
                           @Param("timeTo") LocalDateTime timeTo, @Param("size") long size,
                           @Param("offset") long offset);

    @Select("<script>"
            + "SELECT COUNT(*) FROM kb_knowledge k "
            + "WHERE k.status = 'published' AND k.search_tsv @@ kb_fts_query(#{keyword}) "
            + "<if test='categoryId != null'>AND k.category_id = #{categoryId}</if> "
            + "<if test='tagId != null'>AND EXISTS (SELECT 1 FROM kb_knowledge_tag kt "
            + "WHERE kt.knowledge_id = k.id AND kt.tag_id = #{tagId})</if> "
            + "<if test='timeFrom != null'>AND k.publish_time &gt;= #{timeFrom}</if> "
            + "<if test='timeTo != null'>AND k.publish_time &lt;= #{timeTo}</if>"
            + "</script>")
    long count(@Param("keyword") String keyword, @Param("categoryId") Long categoryId,
               @Param("tagId") Long tagId, @Param("timeFrom") LocalDateTime timeFrom,
               @Param("timeTo") LocalDateTime timeTo);

    /** 搜索命中行。 */
    class SearchRow {
        private Long id;
        private String title;
        private String summary;
        private Long categoryId;
        private String categoryName;
        private String knowledgeType;
        private LocalDateTime publishTime;
        private Integer viewCount;
        private Integer likeCount;
        private Double rank;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }

        public Long getCategoryId() {
            return categoryId;
        }

        public void setCategoryId(Long categoryId) {
            this.categoryId = categoryId;
        }

        public String getCategoryName() {
            return categoryName;
        }

        public void setCategoryName(String categoryName) {
            this.categoryName = categoryName;
        }

        public String getKnowledgeType() {
            return knowledgeType;
        }

        public void setKnowledgeType(String knowledgeType) {
            this.knowledgeType = knowledgeType;
        }

        public LocalDateTime getPublishTime() {
            return publishTime;
        }

        public void setPublishTime(LocalDateTime publishTime) {
            this.publishTime = publishTime;
        }

        public Integer getViewCount() {
            return viewCount;
        }

        public void setViewCount(Integer viewCount) {
            this.viewCount = viewCount;
        }

        public Integer getLikeCount() {
            return likeCount;
        }

        public void setLikeCount(Integer likeCount) {
            this.likeCount = likeCount;
        }

        public Double getRank() {
            return rank;
        }

        public void setRank(Double rank) {
            this.rank = rank;
        }
    }
}
