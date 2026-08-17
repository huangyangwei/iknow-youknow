package com.huangyangwei.iknow.module.knowledge.job;

import com.huangyangwei.iknow.module.knowledge.service.KnowledgePublishService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时发布轮询：周期性扫描 pending_publish 到点条目并发布。
 * 间隔由 iknow.knowledge.scheduled-publish-poll-ms 控制，默认 30s。
 */
@Component
public class ScheduledPublishJob {

    private static final Logger log = LoggerFactory.getLogger(ScheduledPublishJob.class);

    private final KnowledgePublishService publishService;

    public ScheduledPublishJob(KnowledgePublishService publishService) {
        this.publishService = publishService;
    }

    @Scheduled(fixedDelayString = "${iknow.knowledge.scheduled-publish-poll-ms:30000}")
    public void pollPendingPublish() {
        try {
            int published = publishService.publishDueScheduled();
            if (published > 0) {
                log.info("scheduled-publish: published {} pending knowledge entries", published);
            }
        } catch (Exception e) {
            log.warn("scheduled-publish: poll failed", e);
        }
    }
}
