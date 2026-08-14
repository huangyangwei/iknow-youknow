package com.huangyangwei.iknow.module.knowledge.controller;

import com.huangyangwei.iknow.api.dto.knowledge.TagRequest;
import com.huangyangwei.iknow.common.api.Result;
import com.huangyangwei.iknow.common.constant.Constants;
import com.huangyangwei.iknow.common.security.RequireRole;
import com.huangyangwei.iknow.module.knowledge.entity.KbTag;
import com.huangyangwei.iknow.module.knowledge.service.TagService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 标签接口：GET 列表（所有角色可读），创建/删除（管理员）。
 */
@RestController
@RequestMapping("/api/tags")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    public Result<List<KbTag>> list() {
        return Result.ok(tagService.listAll());
    }

    @PostMapping
    @RequireRole(Constants.ROLE_ADMIN)
    public Result<KbTag> create(@RequestBody @Valid TagRequest request) {
        return Result.ok(tagService.create(request));
    }

    @DeleteMapping("/{id}")
    @RequireRole(Constants.ROLE_ADMIN)
    public Result<Void> delete(@PathVariable Long id) {
        tagService.delete(id);
        return Result.ok();
    }
}
