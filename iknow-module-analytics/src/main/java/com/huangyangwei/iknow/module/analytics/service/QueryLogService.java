package com.huangyangwei.iknow.module.analytics.service;

import com.huangyangwei.iknow.common.constant.Constants;
import com.huangyangwei.iknow.module.analytics.entity.StatQueryLog;
import com.huangyangwei.iknow.module.analytics.mapper.StatQueryLogMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 查询日志落库（技术方案 §9.2 stat_query_log）：search / qa。
 * 供检索入口与 QA 完成事件调用，仪表盘据此聚合热门搜索/无结果率/查询趋势。
 */
@Service
public class QueryLogService {

    private static final int KEYWORD_MAX_LENGTH = 255;

    private final StatQueryLogMapper queryLogMapper;

    public QueryLogService(StatQueryLogMapper queryLogMapper) {
        this.queryLogMapper = queryLogMapper;
    }

    @Transactional
    public void record(String queryType, String keyword, boolean hasResult, Long userId) {
        if (!Constants.QUERY_TYPE_SEARCH.equals(queryType) && !Constants.QUERY_TYPE_QA.equals(queryType)) {
            throw new IllegalArgumentException("不支持的查询类型: " + queryType);
        }
        StatQueryLog log = new StatQueryLog();
        log.setQueryType(queryType);
        log.setKeyword(truncate(keyword));
        log.setHasResult(hasResult);
        log.setUserId(userId);
        queryLogMapper.insert(log);
    }

    private String truncate(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.length() > KEYWORD_MAX_LENGTH ? trimmed.substring(0, KEYWORD_MAX_LENGTH) : trimmed;
    }
}
