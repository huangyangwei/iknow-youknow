package com.huangyangwei.iknow.module.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huangyangwei.iknow.api.dto.knowledge.ImportResult;
import com.huangyangwei.iknow.common.api.ResultCode;
import com.huangyangwei.iknow.common.constant.Constants;
import com.huangyangwei.iknow.common.exception.BusinessException;
import com.huangyangwei.iknow.module.knowledge.entity.KbKnowledge;
import com.huangyangwei.iknow.module.knowledge.mapper.KbKnowledgeMapper;
import com.huangyangwei.iknow.module.knowledge.support.HtmlContentConverter;
import com.huangyangwei.iknow.module.knowledge.support.KnowledgeTagSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 批量导入/导出：
 * - 导入：Markdown（.md/.markdown，含 GFM 表格）与 HTML（.html/.htm）批量入库为草稿；
 * - 导出：全部知识条目导出为 JSON 备份文件。
 */
@Service
public class KnowledgeImportExportService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeImportExportService.class);

    private final KbKnowledgeMapper knowledgeMapper;
    private final HtmlContentConverter contentConverter;
    private final KnowledgeTagSupport tagSupport;
    private final ObjectMapper objectMapper;

    public KnowledgeImportExportService(KbKnowledgeMapper knowledgeMapper, HtmlContentConverter contentConverter,
                                        KnowledgeTagSupport tagSupport, ObjectMapper objectMapper) {
        this.knowledgeMapper = knowledgeMapper;
        this.contentConverter = contentConverter;
        this.tagSupport = tagSupport;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ImportResult importFiles(MultipartFile[] files, Long categoryId, Long userId) {
        ImportResult result = new ImportResult();
        result.setTotalCount(files == null ? 0 : files.length);
        int success = 0;
        if (files != null) {
            for (MultipartFile file : files) {
                try {
                    importOne(file, categoryId, userId);
                    success++;
                } catch (Exception e) {
                    log.warn("import failed: file={}, cause={}", file.getOriginalFilename(), e.getMessage());
                    result.getFailures().add(new ImportResult.ImportFailure(
                            file.getOriginalFilename(), e.getMessage()));
                }
            }
        }
        result.setSuccessCount(success);
        return result;
    }

    public byte[] exportBackup() {
        List<KbKnowledge> all = knowledgeMapper.selectList(new LambdaQueryWrapper<KbKnowledge>()
                .orderByAsc(KbKnowledge::getId));
        List<Map<String, Object>> items = new ArrayList<>();
        for (KbKnowledge k : all) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", k.getId());
            item.put("title", k.getTitle());
            item.put("categoryId", k.getCategoryId());
            item.put("knowledgeType", k.getKnowledgeType());
            item.put("status", k.getStatus());
            item.put("versionNo", k.getVersionNo());
            item.put("htmlContent", k.getHtmlContent());
            item.put("plainText", k.getPlainText());
            item.put("summary", k.getSummary());
            item.put("tags", tagSupport.tagNamesOf(k.getId()));
            item.put("publishTime", k.getPublishTime());
            item.put("scheduledPublishTime", k.getScheduledPublishTime());
            item.put("createdBy", k.getCreatedBy());
            item.put("updatedBy", k.getUpdatedBy());
            item.put("createdAt", k.getCreatedAt());
            item.put("updatedAt", k.getUpdatedAt());
            items.add(item);
        }
        Map<String, Object> backup = new LinkedHashMap<>();
        backup.put("app", "iknow-youknow");
        backup.put("exportedAt", LocalDateTime.now().toString());
        backup.put("count", items.size());
        backup.put("items", items);
        try {
            return objectMapper.writeValueAsBytes(backup);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "导出失败: " + e.getMessage());
        }
    }

    private void importOne(MultipartFile file, Long categoryId, Long userId) throws Exception {
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        String filename = file.getOriginalFilename() == null ? "未命名.md" : file.getOriginalFilename();
        String html = toHtml(filename, content);
        String sanitizedHtml = contentConverter.sanitize(html);
        String plainText = contentConverter.toPlainText(sanitizedHtml);

        KbKnowledge k = new KbKnowledge();
        k.setTitle(deriveTitle(filename));
        k.setHtmlContent(sanitizedHtml);
        k.setPlainText(plainText);
        k.setSummary(contentConverter.summarize(plainText));
        k.setCategoryId(categoryId);
        k.setKnowledgeType("FAQ");
        k.setStatus(Constants.KNOWLEDGE_STATUS_DRAFT);
        k.setVersionNo(1);
        k.setViewCount(0);
        k.setLikeCount(0);
        k.setCreatedBy(userId);
        k.setUpdatedBy(userId);
        knowledgeMapper.insert(k);
    }

    private String toHtml(String filename, String content) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".html") || lower.endsWith(".htm")) {
            return content;
        }
        return contentConverter.markdownToHtml(content);
    }

    private String deriveTitle(String filename) {
        String base = filename;
        int dot = base.lastIndexOf('.');
        if (dot > 0) {
            base = base.substring(0, dot);
        }
        return StringUtils.hasText(base) ? base.trim() : "未命名知识";
    }
}
