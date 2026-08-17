package com.huangyangwei.iknow.module.analytics.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 仪表盘聚合查询（技术方案 §9.2：量级小，直接 SQL 聚合）。
 * 覆盖 stat_query_log（热门搜索/无结果率/查询量）、kb_knowledge+kb_category（分类分布）、
 * fb_feedback（反馈趋势/采纳率）。
 */
public interface AnalyticsMapper {

    @Select("<script>"
            + "SELECT keyword AS keyword, COUNT(*) AS count FROM stat_query_log "
            + "WHERE query_type = 'search' AND keyword IS NOT NULL AND keyword &lt;&gt; '' "
            + "<if test='since != null'>AND created_at &gt;= #{since}</if> "
            + "GROUP BY keyword ORDER BY count DESC, keyword ASC LIMIT 10"
            + "</script>")
    List<HotSearchRow> hotSearch(@Param("since") LocalDateTime since);

    @Select("<script>"
            + "SELECT COUNT(*) FROM stat_query_log WHERE query_type = #{queryType} "
            + "<if test='since != null'>AND created_at &gt;= #{since}</if>"
            + "</script>")
    long countQueryByType(@Param("queryType") String queryType, @Param("since") LocalDateTime since);

    @Select("<script>"
            + "SELECT COUNT(*) FROM stat_query_log WHERE has_result = FALSE "
            + "<if test='since != null'>AND created_at &gt;= #{since}</if>"
            + "</script>")
    long countNoResult(@Param("since") LocalDateTime since);

    @Select("<script>"
            + "SELECT COUNT(*) FROM stat_query_log WHERE 1 = 1 "
            + "<if test='since != null'>AND created_at &gt;= #{since}</if>"
            + "</script>")
    long countQuery(@Param("since") LocalDateTime since);

    @Select("SELECT c.id AS categoryId, COALESCE(c.name, '未分类') AS categoryName, COUNT(k.id) AS count "
            + "FROM kb_category c LEFT JOIN kb_knowledge k "
            + "ON k.category_id = c.id AND k.status = 'published' "
            + "GROUP BY c.id, c.name ORDER BY count DESC, c.id ASC")
    List<CategoryDistributionRow> categoryDistribution();

    @Select("<script>"
            + "SELECT to_char(created_at, 'YYYY-MM-DD') AS date, COUNT(*) AS count, "
            + "COUNT(*) FILTER (WHERE status = 'resolved') AS resolvedCount "
            + "FROM fb_feedback WHERE 1 = 1 "
            + "<if test='since != null'>AND created_at &gt;= #{since}</if> "
            + "GROUP BY date ORDER BY date ASC"
            + "</script>")
    List<FeedbackTrendRow> feedbackTrend(@Param("since") LocalDateTime since);

    @Select("SELECT COUNT(*) FILTER (WHERE type = 'like') AS likeCount, "
            + "COUNT(*) FILTER (WHERE type = 'dislike') AS dislikeCount FROM fb_feedback")
    AdoptionRow adoption();

    @Select("<script>"
            + "SELECT COUNT(*) FROM fb_feedback WHERE 1 = 1 "
            + "<if test='since != null'>AND created_at &gt;= #{since}</if>"
            + "</script>")
    long countFeedback(@Param("since") LocalDateTime since);

    @Select("<script>"
            + "SELECT COUNT(*) FROM fb_feedback WHERE status = 'pending' "
            + "<if test='since != null'>AND created_at &gt;= #{since}</if>"
            + "</script>")
    long countPendingFeedback(@Param("since") LocalDateTime since);

    @Select("SELECT COUNT(*) FROM kb_knowledge WHERE status = 'published'")
    long countPublishedKnowledge();

    @Select("SELECT COUNT(*) FROM kb_category")
    long countCategory();

    /** 热门搜索行。 */
    class HotSearchRow {
        private String keyword;
        private long count;

        public String getKeyword() {
            return keyword;
        }

        public void setKeyword(String keyword) {
            this.keyword = keyword;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }
    }

    /** 分类分布行。 */
    class CategoryDistributionRow {
        private Long categoryId;
        private String categoryName;
        private long count;

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

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }
    }

    /** 反馈趋势行。 */
    class FeedbackTrendRow {
        private String date;
        private long count;
        private long resolvedCount;

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }

        public long getResolvedCount() {
            return resolvedCount;
        }

        public void setResolvedCount(long resolvedCount) {
            this.resolvedCount = resolvedCount;
        }
    }

    /** 采纳率行（like/dislike 计数）。 */
    class AdoptionRow {
        private long likeCount;
        private long dislikeCount;

        public long getLikeCount() {
            return likeCount;
        }

        public void setLikeCount(long likeCount) {
            this.likeCount = likeCount;
        }

        public long getDislikeCount() {
            return dislikeCount;
        }

        public void setDislikeCount(long dislikeCount) {
            this.dislikeCount = dislikeCount;
        }
    }
}
