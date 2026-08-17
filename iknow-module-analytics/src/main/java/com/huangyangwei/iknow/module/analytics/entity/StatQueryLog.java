package com.huangyangwei.iknow.module.analytics.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.huangyangwei.iknow.common.entity.BaseEntity;

/**
 * 查询日志（技术方案 §9.2 stat_query_log）：search/qa，仪表盘聚合源。
 */
@TableName("stat_query_log")
public class StatQueryLog extends BaseEntity {

    private Long userId;
    private String queryType;
    private String keyword;
    private Boolean hasResult;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getQueryType() {
        return queryType;
    }

    public void setQueryType(String queryType) {
        this.queryType = queryType;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Boolean getHasResult() {
        return hasResult;
    }

    public void setHasResult(Boolean hasResult) {
        this.hasResult = hasResult;
    }
}
