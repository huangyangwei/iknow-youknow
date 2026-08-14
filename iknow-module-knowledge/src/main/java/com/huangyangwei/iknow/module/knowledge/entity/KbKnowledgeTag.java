package com.huangyangwei.iknow.module.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.huangyangwei.iknow.common.entity.BaseEntity;

/**
 * 知识-标签关联。
 */
@TableName("kb_knowledge_tag")
public class KbKnowledgeTag extends BaseEntity {

    private Long knowledgeId;
    private Long tagId;

    public Long getKnowledgeId() {
        return knowledgeId;
    }

    public void setKnowledgeId(Long knowledgeId) {
        this.knowledgeId = knowledgeId;
    }

    public Long getTagId() {
        return tagId;
    }

    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }
}
