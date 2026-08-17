package com.huangyangwei.iknow.module.analytics.controller;

import com.huangyangwei.iknow.api.dto.analytics.AnalyticsOverview;
import com.huangyangwei.iknow.api.dto.analytics.CategoryDistributionItem;
import com.huangyangwei.iknow.api.dto.analytics.FeedbackTrendItem;
import com.huangyangwei.iknow.api.dto.analytics.HotSearchItem;
import com.huangyangwei.iknow.common.api.Result;
import com.huangyangwei.iknow.common.constant.Constants;
import com.huangyangwei.iknow.common.security.RequireRole;
import com.huangyangwei.iknow.module.analytics.service.AnalyticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 仪表盘接口（技术方案 §7.2）：overview 核心指标卡、hot-search Top10、
 * category-distribution 分类分布、feedback-trend 反馈趋势。管理员可见。
 */
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/overview")
    @RequireRole(Constants.ROLE_ADMIN)
    public Result<AnalyticsOverview> overview(@RequestParam(required = false) String range) {
        return Result.ok(analyticsService.overview(range));
    }

    @GetMapping("/hot-search")
    @RequireRole(Constants.ROLE_ADMIN)
    public Result<List<HotSearchItem>> hotSearch(@RequestParam(required = false) String range) {
        return Result.ok(analyticsService.hotSearch(range));
    }

    @GetMapping("/category-distribution")
    @RequireRole(Constants.ROLE_ADMIN)
    public Result<List<CategoryDistributionItem>> categoryDistribution() {
        return Result.ok(analyticsService.categoryDistribution());
    }

    @GetMapping("/feedback-trend")
    @RequireRole(Constants.ROLE_ADMIN)
    public Result<List<FeedbackTrendItem>> feedbackTrend(@RequestParam(required = false) String range) {
        return Result.ok(analyticsService.feedbackTrend(range));
    }
}
