package com.huangyangwei.iknow.module.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huangyangwei.iknow.api.dto.knowledge.KnowledgeCreateRequest;
import com.huangyangwei.iknow.api.dto.knowledge.KnowledgeDetail;
import com.huangyangwei.iknow.api.dto.knowledge.KnowledgeListItem;
import com.huangyangwei.iknow.api.dto.knowledge.KnowledgeQuery;
import com.huangyangwei.iknow.api.dto.knowledge.KnowledgeUpdateRequest;
import com.huangyangwei.iknow.common.api.PageResult;
import com.huangyangwei.iknow.common.api.ResultCode;
import com.huangyangwei.iknow.common.constant.Constants;
import com.huangyangwei.iknow.common.exception.BusinessException;
import com.huangyangwei.iknow.module.knowledge.entity.KbCategory;
import com.huangyangwei.iknow.module.knowledge.entity.KbKnowledge;
import com.huangyangwei.iknow.module.knowledge.entity.KbKnowledgeTag;
import com.huangyangwei.iknow.module.knowledge.entity.KbKnowledgeVersion;
import com.huangyangwei.iknow.module.knowledge.entity.KbTag;
import com.huangyangwei.iknow.module.knowledge.event.KnowledgeDeletedEvent;
import com.huangyangwei.iknow.module.knowledge.mapper.KbCategoryMapper;
import com.huangyangwei.iknow.module.knowledge.mapper.KbKnowledgeMapper;
import com.huangyangwei.iknow.module.knowledge.mapper.KbKnowledgeTagMapper;
import com.huangyangwei.iknow.module.knowledge.mapper.KbKnowledgeVersionMapper;
import com.huangyangwei.iknow.module.knowledge.mapper.KbTagMapper;
import com.huangyangwei.iknow.module.knowledge.support.HtmlContentConverter;
import com.huangyangwei.iknow.module.knowledge.support.KnowledgeCacheEvictor;
import com.huangyangwei.iknow.module.knowledge.support.KnowledgeTagSupport;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 知识条目服务：列表筛选/分页、详情（富文本双通道）、创建、更新、删除。
 * 发布/回滚/版本由 {@link KnowledgePublishService} 处理；已发布内容变更时自动生成新版本。
 */
@Service
public class KnowledgeService {

    private final KbKnowledgeMapper knowledgeMapper;
    private final KbKnowledgeTagMapper knowledgeTagMapper;
    private final KbKnowledgeVersionMapper versionMapper;
    private final KbTagMapper tagMapper;
    private final KbCategoryMapper categoryMapper;
    private final HtmlContentConverter contentConverter;
    private final KnowledgeTagSupport tagSupport;
    private final KnowledgeCacheEvictor cacheEvictor;
    private final KnowledgePublishService publishService;
    private final ApplicationEventPublisher eventPublisher;

    public KnowledgeService(KbKnowledgeMapper knowledgeMapper, KbKnowledgeTagMapper knowledgeTagMapper,
                            KbKnowledgeVersionMapper versionMapper, KbTagMapper tagMapper,
                            KbCategoryMapper categoryMapper, HtmlContentConverter contentConverter,
                            KnowledgeTagSupport tagSupport, KnowledgeCacheEvictor cacheEvictor,
                            KnowledgePublishService publishService, ApplicationEventPublisher eventPublisher) {
        this.knowledgeMapper = knowledgeMapper;
        this.knowledgeTagMapper = knowledgeTagMapper;
        this.versionMapper = versionMapper;
        this.tagMapper = tagMapper;
        this.categoryMapper = categoryMapper;
        this.contentConverter = contentConverter;
        this.tagSupport = tagSupport;
        this.cacheEvictor = cacheEvictor;
        this.publishService = publishService;
        this.eventPublisher = eventPublisher;
    }

    public PageResult<KnowledgeListItem> page(KnowledgeQuery query) {
        long page = query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage();
        long size = query.getSize() == null || query.getSize() < 1 ? 10 : Math.min(query.getSize(), 200);

        LambdaQueryWrapper<KbKnowledge> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like(KbKnowledge::getTitle, keyword)
                    .or().like(KbKnowledge::getPlainText, keyword)
                    .or().like(KbKnowledge::getSummary, keyword));
        }
        if (query.getCategoryId() != null) {
            wrapper.eq(KbKnowledge::getCategoryId, query.getCategoryId());
        }
        if (query.getTagId() != null) {
            List<Long> knowledgeIds = knowledgeTagMapper.selectList(new LambdaQueryWrapper<KbKnowledgeTag>()
                            .eq(KbKnowledgeTag::getTagId, query.getTagId())).stream()
                    .map(KbKnowledgeTag::getKnowledgeId)
                    .toList();
            if (knowledgeIds.isEmpty()) {
                return PageResult.of(0, page, size, 0, List.of());
            }
            wrapper.in(KbKnowledge::getId, knowledgeIds);
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(KbKnowledge::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getKnowledgeType())) {
            wrapper.eq(KbKnowledge::getKnowledgeType, query.getKnowledgeType());
        }
        applySort(wrapper, query.getSort());

        Page<KbKnowledge> result = knowledgeMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getPages(),
                toListItems(result.getRecords()));
    }

    @Cacheable(value = Constants.CACHE_KNOWLEDGE_DETAIL, key = "#id")
    public KnowledgeDetail detail(Long id) {
        KbKnowledge k = requireKnowledge(id);
        KnowledgeDetail dto = new KnowledgeDetail();
        dto.setId(k.getId());
        dto.setTitle(k.getTitle());
        dto.setSummary(k.getSummary());
        dto.setHtmlContent(k.getHtmlContent());
        dto.setPlainText(k.getPlainText());
        dto.setCategoryId(k.getCategoryId());
        dto.setCategoryName(k.getCategoryId() == null ? null
                : Optional.ofNullable(categoryMapper.selectById(k.getCategoryId()))
                        .map(KbCategory::getName).orElse(null));
        dto.setKnowledgeType(k.getKnowledgeType());
        dto.setStatus(k.getStatus());
        dto.setVersionNo(k.getVersionNo());
        dto.setPublishTime(k.getPublishTime());
        dto.setScheduledPublishTime(k.getScheduledPublishTime());
        dto.setViewCount(k.getViewCount());
        dto.setLikeCount(k.getLikeCount());
        dto.setTags(tagSupport.tagNamesOf(k.getId()));
        dto.setCreatedBy(k.getCreatedBy());
        dto.setUpdatedBy(k.getUpdatedBy());
        dto.setCreatedAt(k.getCreatedAt());
        dto.setUpdatedAt(k.getUpdatedAt());
        return dto;
    }

    @Transactional
    public KnowledgeDetail create(KnowledgeCreateRequest request, Long userId) {
        String html = contentConverter.sanitize(request.getHtmlContent());
        String plainText = contentConverter.toPlainText(html);
        KbKnowledge knowledge = new KbKnowledge();
        knowledge.setTitle(request.getTitle().trim());
        knowledge.setHtmlContent(html);
        knowledge.setPlainText(plainText);
        knowledge.setSummary(StringUtils.hasText(request.getSummary())
                ? request.getSummary().trim()
                : contentConverter.summarize(plainText));
        knowledge.setCategoryId(request.getCategoryId());
        knowledge.setKnowledgeType(StringUtils.hasText(request.getKnowledgeType()) ? request.getKnowledgeType() : "FAQ");
        knowledge.setStatus(Constants.KNOWLEDGE_STATUS_DRAFT);
        if (request.getScheduledPublishTime() != null && request.getScheduledPublishTime().isAfter(LocalDateTime.now())) {
            knowledge.setStatus(Constants.KNOWLEDGE_STATUS_PENDING_PUBLISH);
        }
        knowledge.setVersionNo(1);
        knowledge.setScheduledPublishTime(request.getScheduledPublishTime());
        knowledge.setViewCount(0);
        knowledge.setLikeCount(0);
        knowledge.setCreatedBy(userId);
        knowledge.setUpdatedBy(userId);
        knowledgeMapper.insert(knowledge);
        tagSupport.replaceTags(knowledge.getId(), request.getTagIds());
        return detail(knowledge.getId());
    }

    @Transactional
    public KnowledgeDetail update(Long id, KnowledgeUpdateRequest request, Long userId) {
        KbKnowledge k = requireKnowledge(id);
        boolean contentChanged = false;
        if (StringUtils.hasText(request.getTitle()) && !request.getTitle().trim().equals(k.getTitle())) {
            k.setTitle(request.getTitle().trim());
            contentChanged = true;
        }
        if (request.getHtmlContent() != null) {
            String html = contentConverter.sanitize(request.getHtmlContent());
            if (!html.equals(k.getHtmlContent())) {
                k.setHtmlContent(html);
                k.setPlainText(contentConverter.toPlainText(html));
                contentChanged = true;
            }
        }
        if (request.getSummary() != null) {
            k.setSummary(request.getSummary());
        }
        if (request.getCategoryId() != null) {
            k.setCategoryId(request.getCategoryId());
        }
        if (StringUtils.hasText(request.getKnowledgeType())) {
            k.setKnowledgeType(request.getKnowledgeType());
        }
        if (request.getScheduledPublishTime() != null) {
            k.setScheduledPublishTime(request.getScheduledPublishTime());
        }
        k.setUpdatedBy(userId);
        knowledgeMapper.updateById(k);

        if (request.getTagIds() != null) {
            tagSupport.replaceTags(id, request.getTagIds());
        }

        if (Constants.KNOWLEDGE_STATUS_PUBLISHED.equals(k.getStatus()) && contentChanged) {
            publishService.publishImmediately(id, userId,
                    StringUtils.hasText(request.getChangeNote()) ? request.getChangeNote() : "编辑更新");
        } else {
            cacheEvictor.evictKnowledgeDetail(id);
        }
        return detail(id);
    }

    @Transactional
    public void delete(Long id) {
        requireKnowledge(id);
        knowledgeTagMapper.delete(new LambdaQueryWrapper<KbKnowledgeTag>()
                .eq(KbKnowledgeTag::getKnowledgeId, id));
        versionMapper.delete(new LambdaQueryWrapper<KbKnowledgeVersion>()
                .eq(KbKnowledgeVersion::getKnowledgeId, id));
        knowledgeMapper.deleteById(id);
        cacheEvictor.evictKnowledgeDetail(id);
        eventPublisher.publishEvent(new KnowledgeDeletedEvent(id));
    }

    private void applySort(LambdaQueryWrapper<KbKnowledge> wrapper, String sort) {
        if (!StringUtils.hasText(sort)) {
            wrapper.orderByDesc(KbKnowledge::getUpdatedAt);
            return;
        }
        String[] parts = sort.split(":");
        String field = parts[0];
        boolean asc = parts.length > 1 && "asc".equalsIgnoreCase(parts[1]);
        SFunction<KbKnowledge, ?> column = switch (field) {
            case "created_at" -> KbKnowledge::getCreatedAt;
            case "publish_time" -> KbKnowledge::getPublishTime;
            case "title" -> KbKnowledge::getTitle;
            case "view_count" -> KbKnowledge::getViewCount;
            default -> null;
        };
        if (column == null) {
            wrapper.orderByDesc(KbKnowledge::getUpdatedAt);
        } else if (asc) {
            wrapper.orderByAsc(column);
        } else {
            wrapper.orderByDesc(column);
        }
    }

    /** 批量组装列表项，避免 N+1：一次性查分类名与标签名。 */
    private List<KnowledgeListItem> toListItems(List<KbKnowledge> records) {
        if (records.isEmpty()) {
            return List.of();
        }
        List<Long> categoryIds = records.stream()
                .map(KbKnowledge::getCategoryId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, String> categoryNames = categoryIds.isEmpty() ? Map.of()
                : categoryMapper.selectBatchIds(categoryIds).stream()
                        .collect(Collectors.toMap(KbCategory::getId, KbCategory::getName));

        List<Long> knowledgeIds = records.stream().map(KbKnowledge::getId).toList();
        Map<Long, List<String>> tagNamesByKnowledge = new HashMap<>();
        if (!knowledgeIds.isEmpty()) {
            List<KbKnowledgeTag> links = knowledgeTagMapper.selectList(new LambdaQueryWrapper<KbKnowledgeTag>()
                    .in(KbKnowledgeTag::getKnowledgeId, knowledgeIds));
            List<Long> tagIds = links.stream().map(KbKnowledgeTag::getTagId).distinct().toList();
            Map<Long, String> tagNames = tagIds.isEmpty() ? Map.of()
                    : tagMapper.selectBatchIds(tagIds).stream()
                            .collect(Collectors.toMap(KbTag::getId, KbTag::getName));
            for (KbKnowledgeTag link : links) {
                tagNamesByKnowledge.computeIfAbsent(link.getKnowledgeId(), k -> new ArrayList<>())
                        .add(tagNames.getOrDefault(link.getTagId(), ""));
            }
        }

        return records.stream()
                .map(k -> toListItem(k, categoryNames, tagNamesByKnowledge))
                .toList();
    }

    private KnowledgeListItem toListItem(KbKnowledge k, Map<Long, String> categoryNames,
                                         Map<Long, List<String>> tagNamesByKnowledge) {
        KnowledgeListItem item = new KnowledgeListItem();
        item.setId(k.getId());
        item.setTitle(k.getTitle());
        item.setSummary(k.getSummary());
        item.setCategoryId(k.getCategoryId());
        item.setCategoryName(k.getCategoryId() == null ? null : categoryNames.get(k.getCategoryId()));
        item.setKnowledgeType(k.getKnowledgeType());
        item.setStatus(k.getStatus());
        item.setVersionNo(k.getVersionNo());
        item.setPublishTime(k.getPublishTime());
        item.setScheduledPublishTime(k.getScheduledPublishTime());
        item.setViewCount(k.getViewCount());
        item.setLikeCount(k.getLikeCount());
        item.setTags(tagNamesByKnowledge.getOrDefault(k.getId(), List.of()));
        item.setCreatedBy(k.getCreatedBy());
        item.setUpdatedBy(k.getUpdatedBy());
        item.setCreatedAt(k.getCreatedAt());
        item.setUpdatedAt(k.getUpdatedAt());
        return item;
    }

    private KbKnowledge requireKnowledge(Long id) {
        KbKnowledge knowledge = knowledgeMapper.selectById(id);
        if (knowledge == null) {
            throw new BusinessException(ResultCode.KNOWLEDGE_NOT_FOUND);
        }
        return knowledge;
    }
}
