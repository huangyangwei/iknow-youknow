package com.huangyangwei.iknow.module.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huangyangwei.iknow.api.dto.knowledge.CategoryNode;
import com.huangyangwei.iknow.api.dto.knowledge.CategoryRequest;
import com.huangyangwei.iknow.common.api.ResultCode;
import com.huangyangwei.iknow.common.constant.Constants;
import com.huangyangwei.iknow.common.exception.BusinessException;
import com.huangyangwei.iknow.module.knowledge.entity.KbCategory;
import com.huangyangwei.iknow.module.knowledge.entity.KbKnowledge;
import com.huangyangwei.iknow.module.knowledge.mapper.KbCategoryMapper;
import com.huangyangwei.iknow.module.knowledge.mapper.KbKnowledgeMapper;
import com.huangyangwei.iknow.module.knowledge.support.KnowledgeCacheEvictor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 分类管理：树形查询（列表所有角色可读）、创建/更新/删除（管理员）。
 * 树/血缘缓存于 CACHE_CATEGORY_TREE，写操作整体失效。
 */
@Service
public class CategoryService {

    private final KbCategoryMapper categoryMapper;
    private final KbKnowledgeMapper knowledgeMapper;
    private final KnowledgeCacheEvictor cacheEvictor;

    public CategoryService(KbCategoryMapper categoryMapper, KbKnowledgeMapper knowledgeMapper,
                           KnowledgeCacheEvictor cacheEvictor) {
        this.categoryMapper = categoryMapper;
        this.knowledgeMapper = knowledgeMapper;
        this.cacheEvictor = cacheEvictor;
    }

    @Cacheable(value = Constants.CACHE_CATEGORY_TREE)
    public List<CategoryNode> tree() {
        List<KbCategory> all = categoryMapper.selectList(new LambdaQueryWrapper<KbCategory>()
                .orderByAsc(KbCategory::getSort)
                .orderByAsc(KbCategory::getId));
        Map<Long, List<CategoryNode>> childrenByParent = new HashMap<>();
        Map<Long, CategoryNode> nodeById = new HashMap<>();
        for (KbCategory c : all) {
            CategoryNode node = toNode(c);
            nodeById.put(c.getId(), node);
            childrenByParent.computeIfAbsent(c.getParentId(), k -> new ArrayList<>()).add(node);
        }
        for (CategoryNode node : nodeById.values()) {
            node.setChildren(childrenByParent.getOrDefault(node.getId(), List.of()));
        }
        return childrenByParent.getOrDefault(0L, List.of());
    }

    @Transactional
    @CacheEvict(value = Constants.CACHE_CATEGORY_TREE, allEntries = true)
    public CategoryNode create(CategoryRequest request) {
        KbCategory parent = resolveParent(request.getParentId());
        KbCategory category = new KbCategory();
        category.setParentId(parent == null ? 0L : parent.getId());
        category.setName(request.getName().trim());
        category.setProductLine(request.getProductLine());
        category.setSort(request.getSort() == null ? 0 : request.getSort());
        category.setLevel(parent == null ? 1 : parent.getLevel() + 1);
        categoryMapper.insert(category);
        category.setPath((parent == null ? "" : parent.getPath()) + "/" + category.getId());
        categoryMapper.updateById(category);
        return toNode(category);
    }

    @Transactional
    @CacheEvict(value = Constants.CACHE_CATEGORY_TREE, allEntries = true)
    public void update(Long id, CategoryRequest request) {
        KbCategory category = requireCategory(id);
        if (request.getParentId() != null && !request.getParentId().equals(category.getParentId())) {
            reparent(category, request.getParentId());
        }
        if (request.getName() != null && !request.getName().isBlank()) {
            category.setName(request.getName().trim());
        }
        if (request.getProductLine() != null) {
            category.setProductLine(request.getProductLine());
        }
        if (request.getSort() != null) {
            category.setSort(request.getSort());
        }
        categoryMapper.updateById(category);
        recomputeSubtree(category);
    }

    @Transactional
    @CacheEvict(value = Constants.CACHE_CATEGORY_TREE, allEntries = true)
    public void delete(Long id) {
        requireCategory(id);
        Long childCount = categoryMapper.selectCount(new LambdaQueryWrapper<KbCategory>()
                .eq(KbCategory::getParentId, id));
        if (childCount != null && childCount > 0) {
            throw new BusinessException(ResultCode.KNOWLEDGE_OPERATION_FORBIDDEN, "存在子分类，不能删除");
        }
        Long refCount = knowledgeMapper.selectCount(new LambdaQueryWrapper<KbKnowledge>()
                .eq(KbKnowledge::getCategoryId, id));
        if (refCount != null && refCount > 0) {
            throw new BusinessException(ResultCode.KNOWLEDGE_OPERATION_FORBIDDEN, "分类下存在知识条目，不能删除");
        }
        categoryMapper.deleteById(id);
    }

    private KbCategory resolveParent(Long parentId) {
        if (parentId == null || parentId == 0L) {
            return null;
        }
        KbCategory parent = categoryMapper.selectById(parentId);
        if (parent == null) {
            throw new BusinessException(ResultCode.KNOWLEDGE_NOT_FOUND, "父分类不存在");
        }
        return parent;
    }

    private void reparent(KbCategory category, Long newParentId) {
        if (newParentId == 0L) {
            category.setParentId(0L);
            category.setLevel(1);
            category.setPath("/" + category.getId());
            return;
        }
        if (newParentId.equals(category.getId())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不能将分类挂到自己名下");
        }
        KbCategory parent = requireCategory(newParentId);
        if (isDescendant(parent, category.getId())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不能移动到自己的子分类下");
        }
        category.setParentId(newParentId);
        category.setLevel(parent.getLevel() + 1);
        category.setPath(parent.getPath() + "/" + category.getId());
    }

    /** 移动后级联重算子树 level/path。 */
    private void recomputeSubtree(KbCategory category) {
        List<KbCategory> children = categoryMapper.selectList(new LambdaQueryWrapper<KbCategory>()
                .eq(KbCategory::getParentId, category.getId()));
        for (KbCategory child : children) {
            child.setLevel(category.getLevel() + 1);
            child.setPath(category.getPath() + "/" + child.getId());
            categoryMapper.updateById(child);
            recomputeSubtree(child);
        }
    }

    private boolean isDescendant(KbCategory node, Long ancestorId) {
        KbCategory cursor = node;
        while (cursor != null && cursor.getParentId() != 0L) {
            if (cursor.getParentId().equals(ancestorId)) {
                return true;
            }
            cursor = categoryMapper.selectById(cursor.getParentId());
        }
        return false;
    }

    private KbCategory requireCategory(Long id) {
        KbCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(ResultCode.KNOWLEDGE_NOT_FOUND, "分类不存在");
        }
        return category;
    }

    private CategoryNode toNode(KbCategory c) {
        CategoryNode node = new CategoryNode();
        node.setId(c.getId());
        node.setParentId(c.getParentId());
        node.setName(c.getName());
        node.setProductLine(c.getProductLine());
        node.setSort(c.getSort());
        node.setLevel(c.getLevel());
        node.setPath(c.getPath());
        return node;
    }
}
