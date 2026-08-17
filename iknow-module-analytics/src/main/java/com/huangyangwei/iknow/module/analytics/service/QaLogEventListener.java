package com.huangyangwei.iknow.module.analytics.service;

import com.huangyangwei.iknow.api.event.QaCompletedEvent;
import com.huangyangwei.iknow.common.constant.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 监听问答完成事件（AI 模块发布），写入 stat_query_log（query_type=qa）。
 * 统计落库属旁路职责，异常仅记日志，不影响问答主链路。
 */
@Component
public class QaLogEventListener {

    private static final Logger log = LoggerFactory.getLogger(QaLogEventListener.class);

    private final QueryLogService queryLogService;

    public QaLogEventListener(QueryLogService queryLogService) {
        this.queryLogService = queryLogService;
    }

    @EventListener
    public void onQaCompleted(QaCompletedEvent event) {
        try {
            queryLogService.record(Constants.QUERY_TYPE_QA, event.question(), event.hasResult(), event.userId());
        } catch (Exception e) {
            log.error("记录问答统计失败, question={}", event.question(), e);
        }
    }
}
