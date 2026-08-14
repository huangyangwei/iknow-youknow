package com.huangyangwei.iknow.module.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huangyangwei.iknow.api.dto.knowledge.PublishRequest;
import com.huangyangwei.iknow.api.dto.knowledge.VersionInfo;
import com.huangyangwei.iknow.common.api.ResultCode;
import com.huangyangwei.iknow.common.constant.Constants;
import com.huangyangwei.iknow.common.exception.BusinessException;
import com.huangyangwei.iknow.module.knowledge.entity.KbKnowledge;
import com.huangyangwei.iknow.module.knowledge.entity.KbKnowledgeVersion;
import com.huangyangwei.iknow.module.knowledge.mapper.KbKnowledgeMapper;
import com.huangyangwei.iknow.module.knowledge.mapper.KbKnowledgeVersionMapper;
import com.huangyangwei.iknow.module.knowledge.support.FtsRebuilder;
import com.huangyangwei.iknow.module.knowledge.support.HtmlContentConverter;
import com.huangyangwei.iknow.module.knowledge.support.KnowledgeCacheEvictor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 发布/版本/回滚服务：
 * - 立即发布：快照当前内容为新版本（首次版本号为 1），置 published，重建全文索引并失效缓存；
 * - 定时发布：scheduledTime 为未来时置 pending_publish，由 {@code ScheduledPublishJob} 轮询到点发布；
 * - 回滚：将指定版本快照写回主记录并生成新版本，保证回滚后检索一致；
 * - 版本历史：按版本号倒序返回。
 */
@Service
public class KnowledgePublishService {

    private final KbKnowledgeMapper knowledgeMapper;
    private final KbKnowledgeVersionMapper versionMapper;
    private final HtmlContentConverter contentConverter;
    private final FtsRebuilder ftsRebuilder;
    private final KnowledgeCacheEvictor cacheEvictor;

    public KnowledgePublishService(KbKnowledgeMapper knowledgeMapper, KbKnowledgeVersionMapper versionMapper,
                                   HtmlContentConverter contentConverter, FtsRebuilder ftsRebuilder,
                                   KnowledgeCacheEvictor cacheEvictor) {
        this.knowledgeMapper = knowledgeMapper;
        this.versionMapper = versionMapper;
        this.contentConverter = contentConverter;
        this.ftsRebuilder = ftsRebuilder;
        this.cacheEvictor = cacheEvictor;
    }

    /** 发布入口：未来时间进入定时队列，否则立即发布。 */
    @Transactional
    public void publish(Long id, PublishRequest request, Long userId) {
        KbKnowledge k = requireKnowledge(id);
        if (request != null && request.getScheduledTime() != null
                && request.getScheduledTime().isAfter(LocalDateTime.now())) {
            k.setStatus(Constants.KNOWLEDGE_STATUS_PENDING_PUBLISH);
            k.setScheduledPublishTime(request.getScheduledTime());
            k.setUpdatedBy(userId);
            knowledgeMapper.updateById(k);
            cacheEvictor.evictKnowledgeDetail(id);
            return;
        }
        String changeNote = request != null && StringUtils.hasText(request.getChangeNote())
                ? request.getChangeNote() : "发布";
        publishImmediately(id, userId, changeNote);
    }

    /** 立即发布：快照版本、置 published、失效缓存、重建全文索引。 */
    @Transactional
    public void publishImmediately(Long id, Long userId, String changeNote) {
        KbKnowledge k = requireKnowledge(id);
        String plainText = contentConverter.toPlainText(k.getHtmlContent());
        if (!StringUtils.hasText(k.getSummary())) {
            k.setSummary(contentConverter.summarize(plainText));
        }
        k.setPlainText(plainText);
        k.setStatus(Constants.KNOWLEDGE_STATUS_PUBLISHED);
        k.setPublishTime(LocalDateTime.now());
        k.setScheduledPublishTime(null);
        k.setUpdatedBy(userId);

        int nextVersionNo = nextVersionNo(k, id);
        k.setVersionNo(nextVersionNo);
        knowledgeMapper.updateById(k);

        KbKnowledgeVersion version = new KbKnowledgeVersion();
        version.setKnowledgeId(id);
        version.setVersionNo(nextVersionNo);
        version.setTitle(k.getTitle());
        version.setHtmlContent(k.getHtmlContent());
        version.setPlainText(k.getPlainText());
        version.setSummary(k.getSummary());
        version.setChangeNote(changeNote);
        version.setCreatedBy(userId);
        versionMapper.insert(version);

        cacheEvictor.evictKnowledgeDetail(id);
        ftsRebuilder.rebuild();
    }

    /** 定时发布轮询：到点且状态为 pending_publish 的条目立即发布。 */
    @Transactional
    public int publishDueScheduled() {
        List<KbKnowledge> due = knowledgeMapper.selectList(new LambdaQueryWrapper<KbKnowledge>()
                .eq(KbKnowledge::getStatus, Constants.KNOWLEDGE_STATUS_PENDING_PUBLISH)
                .isNotNull(KbKnowledge::getScheduledPublishTime)
                .le(KbKnowledge::getScheduledPublishTime, LocalDateTime.now()));
        int count = 0;
        for (KbKnowledge k : due) {
            publishImmediately(k.getId(), k.getUpdatedBy(), "定时发布");
            count++;
        }
        return count;
    }

    /** 回滚到指定版本：快照写回主记录并生成新版本。 */
    @Transactional
    public void rollback(Long id, Integer versionNo, Long userId) {
        KbKnowledge k = requireKnowledge(id);
        KbKnowledgeVersion version = versionMapper.selectOne(new LambdaQueryWrapper<KbKnowledgeVersion>()
                .eq(KbKnowledgeVersion::getKnowledgeId, id)
                .eq(KbKnowledgeVersion::getVersionNo, versionNo));
        if (version == null) {
            throw new BusinessException(ResultCode.KNOWLEDGE_NOT_FOUND, "版本不存在: " + versionNo);
        }

        k.setTitle(version.getTitle());
        k.setHtmlContent(version.getHtmlContent());
        k.setPlainText(version.getPlainText());
        k.setSummary(version.getSummary());
        k.setStatus(Constants.KNOWLEDGE_STATUS_PUBLISHED);
        k.setPublishTime(LocalDateTime.now());
        k.setScheduledPublishTime(null);
        k.setUpdatedBy(userId);

        int maxVersion = versionMapper.selectList(new LambdaQueryWrapper<KbKnowledgeVersion>()
                        .eq(KbKnowledgeVersion::getKnowledgeId, id)).stream()
                .mapToInt(KbKnowledgeVersion::getVersionNo)
                .max()
                .orElse(k.getVersionNo() == null ? 0 : k.getVersionNo());
        int newVersionNo = maxVersion + 1;
        k.setVersionNo(newVersionNo);
        knowledgeMapper.updateById(k);

        KbKnowledgeVersion rollbackVersion = new KbKnowledgeVersion();
        rollbackVersion.setKnowledgeId(id);
        rollbackVersion.setVersionNo(newVersionNo);
        rollbackVersion.setTitle(version.getTitle());
        rollbackVersion.setHtmlContent(version.getHtmlContent());
        rollbackVersion.setPlainText(version.getPlainText());
        rollbackVersion.setSummary(version.getSummary());
        rollbackVersion.setChangeNote("回滚自版本 " + versionNo);
        rollbackVersion.setCreatedBy(userId);
        versionMapper.insert(rollbackVersion);

        cacheEvictor.evictKnowledgeDetail(id);
        ftsRebuilder.rebuild();
    }

    public List<VersionInfo> versions(Long id) {
        requireKnowledge(id);
        return versionMapper.selectList(new LambdaQueryWrapper<KbKnowledgeVersion>()
                        .eq(KbKnowledgeVersion::getKnowledgeId, id)
                        .orderByDesc(KbKnowledgeVersion::getVersionNo))
                .stream()
                .map(this::toVersionInfo)
                .toList();
    }

    private int nextVersionNo(KbKnowledge k, Long id) {
        int current = k.getVersionNo() == null ? 1 : k.getVersionNo();
        Long versionCount = versionMapper.selectCount(new LambdaQueryWrapper<KbKnowledgeVersion>()
                .eq(KbKnowledgeVersion::getKnowledgeId, id));
        if (versionCount == null || versionCount == 0) {
            return Math.max(1, current);
        }
        return current + 1;
    }

    private VersionInfo toVersionInfo(KbKnowledgeVersion v) {
        VersionInfo info = new VersionInfo();
        info.setId(v.getId());
        info.setKnowledgeId(v.getKnowledgeId());
        info.setVersionNo(v.getVersionNo());
        info.setTitle(v.getTitle());
        info.setSummary(v.getSummary());
        info.setChangeNote(v.getChangeNote());
        info.setCreatedBy(v.getCreatedBy());
        info.setCreatedAt(v.getCreatedAt());
        return info;
    }

    private KbKnowledge requireKnowledge(Long id) {
        KbKnowledge knowledge = knowledgeMapper.selectById(id);
        if (knowledge == null) {
            throw new BusinessException(ResultCode.KNOWLEDGE_NOT_FOUND);
        }
        return knowledge;
    }
}
