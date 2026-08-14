package com.huangyangwei.iknow.module.knowledge.controller;

import com.huangyangwei.iknow.api.dto.knowledge.ImportResult;
import com.huangyangwei.iknow.api.dto.knowledge.KnowledgeCreateRequest;
import com.huangyangwei.iknow.api.dto.knowledge.KnowledgeDetail;
import com.huangyangwei.iknow.api.dto.knowledge.KnowledgeListItem;
import com.huangyangwei.iknow.api.dto.knowledge.KnowledgeQuery;
import com.huangyangwei.iknow.api.dto.knowledge.KnowledgeUpdateRequest;
import com.huangyangwei.iknow.api.dto.knowledge.PublishRequest;
import com.huangyangwei.iknow.api.dto.knowledge.RollbackRequest;
import com.huangyangwei.iknow.api.dto.knowledge.VersionInfo;
import com.huangyangwei.iknow.common.api.PageResult;
import com.huangyangwei.iknow.common.api.Result;
import com.huangyangwei.iknow.common.constant.Constants;
import com.huangyangwei.iknow.common.security.RequireRole;
import com.huangyangwei.iknow.module.knowledge.service.KnowledgeImportExportService;
import com.huangyangwei.iknow.module.knowledge.service.KnowledgePublishService;
import com.huangyangwei.iknow.module.knowledge.service.KnowledgeService;
import com.huangyangwei.iknow.module.knowledge.support.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

/**
 * 知识接口：列表/详情所有角色可读；创建/更新/删除/发布/回滚/导入导出为管理员操作。
 * 详情返回 html_content（展示）与 plain_text（检索）双通道。
 */
@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;
    private final KnowledgePublishService publishService;
    private final KnowledgeImportExportService importExportService;

    public KnowledgeController(KnowledgeService knowledgeService, KnowledgePublishService publishService,
                               KnowledgeImportExportService importExportService) {
        this.knowledgeService = knowledgeService;
        this.publishService = publishService;
        this.importExportService = importExportService;
    }

    @GetMapping
    public Result<PageResult<KnowledgeListItem>> list(KnowledgeQuery query) {
        return Result.ok(knowledgeService.page(query));
    }

    @PostMapping
    @RequireRole(Constants.ROLE_ADMIN)
    public Result<KnowledgeDetail> create(@RequestBody @Valid KnowledgeCreateRequest request) {
        return Result.ok(knowledgeService.create(request, SecurityUtils.currentUser().id()));
    }

    @GetMapping("/{id}")
    public Result<KnowledgeDetail> detail(@PathVariable Long id) {
        return Result.ok(knowledgeService.detail(id));
    }

    @PutMapping("/{id}")
    @RequireRole(Constants.ROLE_ADMIN)
    public Result<KnowledgeDetail> update(@PathVariable Long id, @RequestBody @Valid KnowledgeUpdateRequest request) {
        return Result.ok(knowledgeService.update(id, request, SecurityUtils.currentUser().id()));
    }

    @DeleteMapping("/{id}")
    @RequireRole(Constants.ROLE_ADMIN)
    public Result<Void> delete(@PathVariable Long id) {
        knowledgeService.delete(id);
        return Result.ok();
    }

    @PostMapping("/{id}/publish")
    @RequireRole(Constants.ROLE_ADMIN)
    public Result<Void> publish(@PathVariable Long id,
                                @RequestBody(required = false) PublishRequest request) {
        publishService.publish(id, request, SecurityUtils.currentUser().id());
        return Result.ok();
    }

    @PostMapping("/{id}/rollback")
    @RequireRole(Constants.ROLE_ADMIN)
    public Result<Void> rollback(@PathVariable Long id, @RequestBody @Valid RollbackRequest request) {
        publishService.rollback(id, request.getVersionNo(), SecurityUtils.currentUser().id());
        return Result.ok();
    }

    @GetMapping("/{id}/versions")
    @RequireRole(Constants.ROLE_ADMIN)
    public Result<List<VersionInfo>> versions(@PathVariable Long id) {
        return Result.ok(publishService.versions(id));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequireRole(Constants.ROLE_ADMIN)
    public Result<ImportResult> importFiles(@RequestParam("files") MultipartFile[] files,
                                            @RequestParam(value = "categoryId", required = false) Long categoryId) {
        return Result.ok(importExportService.importFiles(files, categoryId, SecurityUtils.currentUser().id()));
    }

    @GetMapping("/export")
    @RequireRole(Constants.ROLE_ADMIN)
    public ResponseEntity<byte[]> export() {
        byte[] content = importExportService.exportBackup();
        String filename = "knowledge-backup-" + LocalDate.now() + ".json";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(content);
    }
}
