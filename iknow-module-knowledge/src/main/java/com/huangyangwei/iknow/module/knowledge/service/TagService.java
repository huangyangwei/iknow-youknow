package com.huangyangwei.iknow.module.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huangyangwei.iknow.api.dto.knowledge.TagRequest;
import com.huangyangwei.iknow.common.api.ResultCode;
import com.huangyangwei.iknow.common.constant.Constants;
import com.huangyangwei.iknow.common.exception.BusinessException;
import com.huangyangwei.iknow.module.knowledge.entity.KbKnowledgeTag;
import com.huangyangwei.iknow.module.knowledge.entity.KbTag;
import com.huangyangwei.iknow.module.knowledge.mapper.KbKnowledgeTagMapper;
import com.huangyangwei.iknow.module.knowledge.mapper.KbTagMapper;
import com.huangyangwei.iknow.module.knowledge.support.KnowledgeCacheEvictor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 标签管理：字典查询（所有角色可读）、创建/删除（管理员）。
 */
@Service
public class TagService {

    private final KbTagMapper tagMapper;
    private final KbKnowledgeTagMapper knowledgeTagMapper;
    private final KnowledgeCacheEvictor cacheEvictor;

    public TagService(KbTagMapper tagMapper, KbKnowledgeTagMapper knowledgeTagMapper,
                      KnowledgeCacheEvictor cacheEvictor) {
        this.tagMapper = tagMapper;
        this.knowledgeTagMapper = knowledgeTagMapper;
        this.cacheEvictor = cacheEvictor;
    }

    @Cacheable(value = Constants.CACHE_TAG_DICT)
    public List<KbTag> listAll() {
        return tagMapper.selectList(new LambdaQueryWrapper<KbTag>()
                .orderByAsc(KbTag::getId));
    }

    @Transactional
    @CacheEvict(value = Constants.CACHE_TAG_DICT, allEntries = true)
    public KbTag create(TagRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "标签名称不能为空");
        }
        String name = request.getName().trim();
        Long exists = tagMapper.selectCount(new LambdaQueryWrapper<KbTag>()
                .eq(KbTag::getName, name));
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.KNOWLEDGE_OPERATION_FORBIDDEN, "标签已存在: " + name);
        }
        KbTag tag = new KbTag();
        tag.setName(name);
        tagMapper.insert(tag);
        return tag;
    }

    @Transactional
    @CacheEvict(value = Constants.CACHE_TAG_DICT, allEntries = true)
    public void delete(Long id) {
        if (tagMapper.selectById(id) == null) {
            throw new BusinessException(ResultCode.KNOWLEDGE_NOT_FOUND, "标签不存在");
        }
        knowledgeTagMapper.delete(new LambdaQueryWrapper<KbKnowledgeTag>()
                .eq(KbKnowledgeTag::getTagId, id));
        tagMapper.deleteById(id);
    }
}
