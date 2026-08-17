package com.huangyangwei.iknow.module.analytics.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huangyangwei.iknow.api.dto.search.SearchResultItem;
import com.huangyangwei.iknow.common.api.PageResult;
import com.huangyangwei.iknow.common.constant.Constants;
import com.huangyangwei.iknow.module.analytics.mapper.SearchMapper;
import com.huangyangwei.iknow.module.analytics.mapper.SearchMapper.SearchRow;
import com.huangyangwei.iknow.module.knowledge.entity.KbKnowledgeTag;
import com.huangyangwei.iknow.module.knowledge.entity.KbTag;
import com.huangyangwei.iknow.module.knowledge.mapper.KbKnowledgeTagMapper;
import com.huangyangwei.iknow.module.knowledge.mapper.KbTagMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 检索服务（技术方案 §7.2 GET /api/search）：published 知识全文检索 + 筛选 + 分页，
 * 并写入 stat_query_log（query_type=search，has_result 按命中数）。
 */
@Service
public class SearchService {

    private final SearchMapper searchMapper;
    private final KbKnowledgeTagMapper knowledgeTagMapper;
    private final KbTagMapper tagMapper;
    private final QueryLogService queryLogService;

    public SearchService(SearchMapper searchMapper, KbKnowledgeTagMapper knowledgeTagMapper,
                         KbTagMapper tagMapper, QueryLogService queryLogService) {
        this.searchMapper = searchMapper;
        this.knowledgeTagMapper = knowledgeTagMapper;
        this.tagMapper = tagMapper;
        this.queryLogService = queryLogService;
    }

    public PageResult<SearchResultItem> search(String keyword, Long categoryId, Long tagId,
                                               LocalDateTime timeFrom, LocalDateTime timeTo,
                                               long page, long size, Long userId) {
        long safePage = page < 1 ? 1 : page;
        long safeSize = size < 1 ? 10 : Math.min(size, 200);
        if (!StringUtils.hasText(keyword)) {
            return PageResult.of(0, safePage, safeSize, 0, List.of());
        }
        String kw = keyword.trim();

        long total = searchMapper.count(kw, categoryId, tagId, timeFrom, timeTo);
        long offset = (safePage - 1) * safeSize;
        List<SearchRow> rows = total == 0 ? List.of()
                : searchMapper.search(kw, categoryId, tagId, timeFrom, timeTo, safeSize, offset);

        Map<Long, List<String>> tagsByKnowledge = loadTags(rows.stream().map(SearchRow::getId).toList());
        List<SearchResultItem> records = rows.stream()
                .map(row -> toItem(row, tagsByKnowledge))
                .toList();

        try {
            queryLogService.record(Constants.QUERY_TYPE_SEARCH, kw, total > 0, userId);
        } catch (Exception e) {
            // 统计落库旁路失败不影响检索结果
        }
        return PageResult.of(total, safePage, safeSize, total == 0 ? 0 : (total + safeSize - 1) / safeSize, records);
    }

    private Map<Long, List<String>> loadTags(List<Long> knowledgeIds) {
        if (knowledgeIds.isEmpty()) {
            return Map.of();
        }
        List<KbKnowledgeTag> links = knowledgeTagMapper.selectList(new LambdaQueryWrapper<KbKnowledgeTag>()
                .in(KbKnowledgeTag::getKnowledgeId, knowledgeIds));
        List<Long> tagIds = links.stream().map(KbKnowledgeTag::getTagId).distinct().toList();
        Map<Long, String> tagNames = tagIds.isEmpty() ? Map.of()
                : tagMapper.selectBatchIds(tagIds).stream()
                        .collect(Collectors.toMap(KbTag::getId, KbTag::getName));
        Map<Long, List<String>> result = new HashMap<>();
        for (KbKnowledgeTag link : links) {
            result.computeIfAbsent(link.getKnowledgeId(), k -> new ArrayList<>())
                    .add(tagNames.getOrDefault(link.getTagId(), ""));
        }
        return result;
    }

    private SearchResultItem toItem(SearchRow row, Map<Long, List<String>> tagsByKnowledge) {
        SearchResultItem item = new SearchResultItem();
        item.setId(row.getId());
        item.setTitle(row.getTitle());
        item.setSummary(row.getSummary());
        item.setCategoryId(row.getCategoryId());
        item.setCategoryName(row.getCategoryName());
        item.setKnowledgeType(row.getKnowledgeType());
        item.setPublishTime(row.getPublishTime());
        item.setViewCount(row.getViewCount());
        item.setLikeCount(row.getLikeCount());
        item.setRank(row.getRank());
        item.setTags(tagsByKnowledge.getOrDefault(row.getId(), List.of()));
        return item;
    }
}
