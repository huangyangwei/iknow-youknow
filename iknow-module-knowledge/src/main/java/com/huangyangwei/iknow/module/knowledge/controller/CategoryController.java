package com.huangyangwei.iknow.module.knowledge.controller;

import com.huangyangwei.iknow.api.dto.knowledge.CategoryNode;
import com.huangyangwei.iknow.api.dto.knowledge.CategoryRequest;
import com.huangyangwei.iknow.common.api.Result;
import com.huangyangwei.iknow.common.constant.Constants;
import com.huangyangwei.iknow.common.security.RequireRole;
import com.huangyangwei.iknow.module.knowledge.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 分类接口：GET 树（所有角色可读），增删改（管理员）。
 */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public Result<List<CategoryNode>> tree() {
        return Result.ok(categoryService.tree());
    }

    @PostMapping
    @RequireRole(Constants.ROLE_ADMIN)
    public Result<CategoryNode> create(@RequestBody @Valid CategoryRequest request) {
        return Result.ok(categoryService.create(request));
    }

    @PutMapping("/{id}")
    @RequireRole(Constants.ROLE_ADMIN)
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid CategoryRequest request) {
        categoryService.update(id, request);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequireRole(Constants.ROLE_ADMIN)
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.ok();
    }
}
