package com.huangyangwei.iknow.module.knowledge.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huangyangwei.iknow.module.knowledge.entity.KbKnowledgeTag;
import com.huangyangwei.iknow.module.knowledge.entity.KbTag;
import com.huangyangwei.iknow.module.knowledge.mapper.KbKnowledgeTagMapper;
import com.huangyangwei.iknow.module.knowledge.mapper.KbTagMapper;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 知识-标签关联操作：查询标签名列表、整体替换标签集合。
 */
@Component
public class KnowledgeTagSupport {

    private final KbKnowledgeTagMapper knowledgeTagMapper;
    private final KbTagMapper tagMapper;

    public KnowledgeTagSupport(KbKnowledgeTagMapper knowledgeTagMapper, KbTagMapper tagMapper) {
        this.knowledgeTagMapper = knowledgeTagMapper;
        this.tagMapper = tagMapper;
    }

    public List<String> tagNamesOf(Long knowledgeId) {
        List<Long> tagIds = knowledgeTagMapper.selectList(new LambdaQueryWrapper<KbKnowledgeTag>()
                        .eq(KbKnowledgeTag::getKnowledgeId, knowledgeId)).stream()
                .map(KbKnowledgeTag::getTagId)
                .toList();
        if (tagIds.isEmpty()) {
            return List.of();
        }
        return tagMapper.selectBatchIds(tagIds).stream()
                .map(KbTag::getName)
                .toList();
    }

    public void replaceTags(Long knowledgeId, List<Long> tagIds) {
        knowledgeTagMapper.delete(new LambdaQueryWrapper<KbKnowledgeTag>()
                .eq(KbKnowledgeTag::getKnowledgeId, knowledgeId));
        if (tagIds == null) {
            return;
        }
        for (Long tagId : tagIds) {
            if (tagId == null) {
                continue;
            }
            KbKnowledgeTag link = new KbKnowledgeTag();
            link.setKnowledgeId(knowledgeId);
            link.setTagId(tagId);
            knowledgeTagMapper.insert(link);
        }
    }
}
