package com.huangyangwei.iknow.module.ai.listener;

import com.huangyangwei.iknow.module.ai.service.ChunkVectorizationService;
import com.huangyangwei.iknow.module.knowledge.event.KnowledgeDeletedEvent;
import com.huangyangwei.iknow.module.knowledge.event.KnowledgePublishedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 发布/删除事件 → 向量化重建/清理（事务提交后执行，失败不阻断发布业务）。
 */
@Component
public class KnowledgeRagListener {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeRagListener.class);

    private final ChunkVectorizationService chunkVectorizationService;

    public KnowledgeRagListener(ChunkVectorizationService chunkVectorizationService) {
        this.chunkVectorizationService = chunkVectorizationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPublished(KnowledgePublishedEvent event) {
        try {
            chunkVectorizationService.reindexKnowledge(event.knowledgeId(), event.versionNo(),
                    event.title(), event.plainText());
        } catch (Exception e) {
            log.error("vectorize on publish failed for knowledge {}", event.knowledgeId(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onDeleted(KnowledgeDeletedEvent event) {
        try {
            chunkVectorizationService.deleteByKnowledgeId(event.knowledgeId());
        } catch (Exception e) {
            log.error("delete chunks on delete failed for knowledge {}", event.knowledgeId(), e);
        }
    }
}
