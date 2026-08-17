package com.huangyangwei.iknow.module.analytics.controller;

import com.huangyangwei.iknow.api.dto.search.SearchResultItem;
import com.huangyangwei.iknow.common.api.PageResult;
import com.huangyangwei.iknow.common.api.Result;
import com.huangyangwei.iknow.module.analytics.service.SearchService;
import com.huangyangwei.iknow.module.knowledge.support.SecurityUtils;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 检索接口（技术方案 §7.2 GET /api/search）：全文检索 + 分类/标签/时间筛选 + 分页；
 * 写入 stat_query_log 供仪表盘热门搜索/无结果率聚合。
 */
@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public Result<PageResult<SearchResultItem>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime timeFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime timeTo,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(searchService.search(keyword, categoryId, tagId, timeFrom, timeTo,
                page, size, SecurityUtils.currentUser().id()));
    }
}
