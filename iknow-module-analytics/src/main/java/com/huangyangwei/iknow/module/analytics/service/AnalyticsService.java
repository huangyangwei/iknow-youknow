package com.huangyangwei.iknow.module.analytics.service;

import com.huangyangwei.iknow.api.dto.analytics.AnalyticsOverview;
import com.huangyangwei.iknow.api.dto.analytics.CategoryDistributionItem;
import com.huangyangwei.iknow.api.dto.analytics.FeedbackTrendItem;
import com.huangyangwei.iknow.api.dto.analytics.HotSearchItem;
import com.huangyangwei.iknow.common.constant.Constants;
import com.huangyangwei.iknow.module.analytics.mapper.AnalyticsMapper;
import com.huangyangwei.iknow.module.analytics.mapper.AnalyticsMapper.AdoptionRow;
import com.huangyangwei.iknow.module.analytics.mapper.AnalyticsMapper.CategoryDistributionRow;
import com.huangyangwei.iknow.module.analytics.mapper.AnalyticsMapper.FeedbackTrendRow;
import com.huangyangwei.iknow.module.analytics.mapper.AnalyticsMapper.HotSearchRow;
import com.huangyangwei.iknow.module.analytics.support.AnalyticsRange;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 仪表盘聚合（技术方案 §7.2/§9.2）：核心指标卡、热门搜索、分类分布、反馈趋势。
 * 量级小，直接 SQL 聚合，无需数仓。采纳率 = like / (like + dislike)。
 */
@Service
public class AnalyticsService {

    private final AnalyticsMapper analyticsMapper;

    public AnalyticsService(AnalyticsMapper analyticsMapper) {
        this.analyticsMapper = analyticsMapper;
    }

    public AnalyticsOverview overview(String range) {
        LocalDateTime since = AnalyticsRange.since(range);
        AnalyticsOverview overview = new AnalyticsOverview();
        overview.setKnowledgeCount(analyticsMapper.countPublishedKnowledge());
        overview.setCategoryCount(analyticsMapper.countCategory());
        overview.setFeedbackCount(analyticsMapper.countFeedback(since));
        overview.setPendingFeedbackCount(analyticsMapper.countPendingFeedback(since));

        long queryCount = analyticsMapper.countQuery(since);
        overview.setQueryCount(queryCount);
        overview.setSearchCount(analyticsMapper.countQueryByType(Constants.QUERY_TYPE_SEARCH, since));
        overview.setQaCount(analyticsMapper.countQueryByType(Constants.QUERY_TYPE_QA, since));
        overview.setNoResultRate(queryCount == 0 ? 0
                : round4((double) analyticsMapper.countNoResult(since) / queryCount));

        AdoptionRow adoption = analyticsMapper.adoption();
        overview.setLikeCount(adoption.getLikeCount());
        overview.setDislikeCount(adoption.getDislikeCount());
        long judgement = adoption.getLikeCount() + adoption.getDislikeCount();
        overview.setAdoptionRate(judgement == 0 ? 0 : round4((double) adoption.getLikeCount() / judgement));
        return overview;
    }

    public List<HotSearchItem> hotSearch(String range) {
        return analyticsMapper.hotSearch(AnalyticsRange.since(range)).stream()
                .map(row -> new HotSearchItem(row.getKeyword(), row.getCount()))
                .toList();
    }

    public List<CategoryDistributionItem> categoryDistribution() {
        return analyticsMapper.categoryDistribution().stream()
                .map(row -> new CategoryDistributionItem(row.getCategoryId(), row.getCategoryName(), row.getCount()))
                .toList();
    }

    public List<FeedbackTrendItem> feedbackTrend(String range) {
        return analyticsMapper.feedbackTrend(AnalyticsRange.since(range)).stream()
                .map(row -> new FeedbackTrendItem(row.getDate(), row.getCount(), row.getResolvedCount()))
                .toList();
    }

    private double round4(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }
}
