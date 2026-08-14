package com.huangyangwei.iknow;

import com.huangyangwei.iknow.common.util.JwtUtil;
import com.huangyangwei.iknow.module.knowledge.service.KnowledgePublishService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 知识域 P1 验收测试（dev profile：嵌入式 PG16 + Flyway V1/V2）：
 * - 分类/标签/知识 CRUD + 富文本双通道（html_content 展示 / plain_text 检索）
 * - 发布→全文索引可检索 + 版本快照；编辑发布内容自动升版；回滚后检索一致
 * - 定时发布（pending_publish → 到点发布）
 * - 导入（Markdown/HTML）/导出备份
 * - RBAC：列表所有角色可读，管理操作仅管理员
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class KnowledgeModuleIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private KnowledgePublishService publishService;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private String post(String path, String json, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json));
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString()).body();
    }

    private String put(String path, String json, String token) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString()).body();
    }

    private String get(String path, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .GET();
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString()).body();
    }

    private HttpResponse<String> rawGet(String path, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .GET();
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String login() throws Exception {
        return extractToken(post("/api/auth/login",
                "{\"email\":\"admin@iknow.ai\",\"password\":\"Admin@123\"}", null));
    }

    private String extractToken(String loginBody) {
        String marker = "\"accessToken\":\"";
        int start = loginBody.indexOf(marker);
        assertTrue(start >= 0, "login body missing accessToken: " + loginBody);
        start += marker.length();
        int end = loginBody.indexOf('"', start);
        return loginBody.substring(start, end);
    }

    private long jsonId(String body) throws Exception {
        JsonNode node = objectMapper.readTree(body);
        assertTrue(node.path("code").asInt() == 0, "unexpected response: " + body);
        return node.path("data").path("id").asLong();
    }

    @Test
    void categoryTagKnowledgePublishSearchVersionRollback() throws Exception {
        String token = login();

        // 分类 + 标签
        long categoryId = jsonId(post("/api/categories",
                "{\"parentId\":0,\"name\":\"技术文档\",\"productLine\":\"RAG\",\"sort\":1}", token));
        long tagId = jsonId(post("/api/tags", "{\"name\":\"检索\"}", token));
        assertTrue(categoryId > 0 && tagId > 0);

        // 分类树可读（所有角色），且包含新分类
        String treeBody = get("/api/categories", token);
        assertTrue(treeBody.contains("\"code\":0"), treeBody);
        assertTrue(treeBody.contains("技术文档"), treeBody);

        // 创建知识（草稿）：双通道 html_content + 自动生成 plain_text
        String tokenWord = "quantum" + randomSuffix();
        String createBody = post("/api/knowledge",
                "{\"title\":\"RAG 检索原理\",\"htmlContent\":\"<p>" + tokenWord + " RAG 原理介绍</p>\","
                        + "\"categoryId\":" + categoryId + ",\"knowledgeType\":\"FAQ\",\"tagIds\":[" + tagId + "]}",
                token);
        long knowledgeId = jsonId(createBody);

        String detailDraft = get("/api/knowledge/" + knowledgeId, token);
        assertTrue(detailDraft.contains("\"status\":\"draft\""), detailDraft);
        assertTrue(detailDraft.contains(tokenWord), detailDraft);
        assertTrue(detailDraft.contains("\"plainText\":\"" + tokenWord + " RAG 原理介绍\""), detailDraft);

        // 发布 → published、版本 1、全文索引可检索、缓存已失效
        String publishBody = post("/api/knowledge/" + knowledgeId + "/publish", "{}", token);
        assertTrue(publishBody.contains("\"code\":0"), publishBody);

        String detailPublished = get("/api/knowledge/" + knowledgeId, token);
        assertTrue(detailPublished.contains("\"status\":\"published\""), detailPublished);
        assertTrue(detailPublished.contains("\"versionNo\":1"), detailPublished);
        assertTrue(detailPublished.contains("\"publishTime\""), detailPublished);
        assertTrue(detailPublished.contains("\"categoryName\":\"技术文档\""), detailPublished);
        assertTrue(detailPublished.contains("\"tags\":[\"检索\"]"), detailPublished);

        assertEquals(1, ftsCount(tokenWord), "FTS should match published content");
        String listBody = get("/api/knowledge?keyword=" + tokenWord, token);
        assertTrue(listBody.contains("\"code\":0"), listBody);
        assertTrue(listBody.contains(tokenWord), listBody);

        // 编辑已发布内容 → 自动升版为 2，检索切换为新内容
        String newWord = "newword" + randomSuffix();
        put("/api/knowledge/" + knowledgeId,
                "{\"title\":\"RAG 检索原理\",\"htmlContent\":\"<p>" + newWord + " 更新后的内容</p>\"}", token);
        assertTrue(get("/api/knowledge/" + knowledgeId, token).contains("\"versionNo\":2"), "expected version bump to 2");
        assertEquals(0, ftsCount(tokenWord), "old token should be gone after republish");
        assertEquals(1, ftsCount(newWord), "new token should be searchable after republish");

        // 版本历史：2 → 1
        String versions = get("/api/knowledge/" + knowledgeId + "/versions", token);
        assertTrue(versions.contains("\"versionNo\":2"), versions);
        assertTrue(versions.contains("\"versionNo\":1"), versions);

        // 回滚到版本 1 → 内容回到 v1、生成版本 3、检索一致
        String rollback = post("/api/knowledge/" + knowledgeId + "/rollback",
                "{\"versionNo\":1}", token);
        assertTrue(rollback.contains("\"code\":0"), rollback);
        String detailAfterRollback = get("/api/knowledge/" + knowledgeId, token);
        assertTrue(detailAfterRollback.contains("\"versionNo\":3"), detailAfterRollback);
        assertTrue(detailAfterRollback.contains(tokenWord), detailAfterRollback);
        assertEquals(1, ftsCount(tokenWord), "rollback should restore v1 searchable content");
        assertEquals(0, ftsCount(newWord), "rolled-back content should no longer match new token");

        // 删除
        String deleteBody = delete("/api/knowledge/" + knowledgeId, token);
        assertTrue(deleteBody.contains("\"code\":0"), deleteBody);
        assertTrue(get("/api/knowledge/" + knowledgeId, token).contains("\"code\":3000"), "deleted detail should be 3000");
    }

    @Test
    void scheduledPublishPublishesWhenDue() throws Exception {
        String token = login();
        long knowledgeId = jsonId(post("/api/knowledge",
                "{\"title\":\"定时发布条目\",\"htmlContent\":\"<p>scheduledword 定时内容</p>\"}", token));

        // 未来时间 → pending_publish
        String scheduled = post("/api/knowledge/" + knowledgeId + "/publish",
                "{\"scheduledTime\":\"2099-01-01T00:00:00\",\"changeNote\":\"定时发布\"}", token);
        assertTrue(scheduled.contains("\"code\":0"), scheduled);
        assertTrue(get("/api/knowledge/" + knowledgeId, token).contains("\"status\":\"pending_publish\""),
                "should be pending_publish");

        // 将到点时间回拨到过去，触发轮询发布
        jdbcTemplate.update("UPDATE kb_knowledge SET scheduled_publish_time = now() - interval '1 minute' WHERE id = ?",
                knowledgeId);
        publishService.publishDueScheduled();

        String detail = get("/api/knowledge/" + knowledgeId, token);
        assertTrue(detail.contains("\"status\":\"published\""), detail);
        assertTrue(detail.contains("\"versionNo\":1"), detail);
        assertTrue(get("/api/knowledge/" + knowledgeId + "/versions", token).contains("定时发布"), "version changeNote");
    }

    @Test
    void memberCanReadButCannotWrite() throws Exception {
        String memberToken = jwtUtil.generate(999L, "member", List.of("MEMBER"), List.of());

        String listBody = get("/api/knowledge", memberToken);
        assertTrue(listBody.contains("\"code\":0"), "list should be readable by all roles: " + listBody);

        String createBody = post("/api/knowledge",
                "{\"title\":\"越权创建\",\"htmlContent\":\"<p>x</p>\"}", memberToken);
        assertTrue(createBody.contains("\"code\":2003"), "member create should be FORBIDDEN: " + createBody);

        String categoryBody = post("/api/categories", "{\"name\":\"越权分类\"}", memberToken);
        assertTrue(categoryBody.contains("\"code\":2003"), "member category create should be FORBIDDEN: " + categoryBody);
    }

    @Test
    void importMarkdownAndHtmlThenExportBackup() throws Exception {
        String token = login();
        long categoryId = jsonId(post("/api/categories", "{\"name\":\"导入分类\"}", token));

        String markdown = "# 导入标题\n\n这是一段 Markdown 内容\n\n| A | B |\n|---|---|\n| 1 | 2 |\n";
        String html = "<h1>HTML 导入</h1><p>一段 HTML 内容 importword" + randomSuffix() + "</p>";

        String boundary = "----IKnow" + UUID.randomUUID().toString().replace("-", "");
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("categoryId", String.valueOf(categoryId));
        Map<String, byte[]> files = new LinkedHashMap<>();
        files.put("import-doc.md", markdown.getBytes(StandardCharsets.UTF_8));
        files.put("import-page.html", html.getBytes(StandardCharsets.UTF_8));
        byte[] body = buildMultipart(boundary, fields, files);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/knowledge/import"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
        String importBody = client.send(request, HttpResponse.BodyHandlers.ofString()).body();
        assertTrue(importBody.contains("\"code\":0"), importBody);
        assertTrue(importBody.contains("\"successCount\":2"), importBody);

        // 导入产物为草稿：标题取自文件名；Markdown 已转为 HTML 富文本
        String list = get("/api/knowledge?status=draft", token);
        JsonNode records = objectMapper.readTree(list).path("data").path("records");
        String mdId = null;
        String htmlId = null;
        for (JsonNode rec : records) {
            String title = rec.path("title").asText();
            if ("import-doc".equals(title)) {
                mdId = rec.path("id").asText();
            }
            if ("import-page".equals(title)) {
                htmlId = rec.path("id").asText();
            }
        }
        assertNotNull(mdId, "markdown import should appear in list");
        assertNotNull(htmlId, "html import should appear in list");
        String mdDetail = get("/api/knowledge/" + mdId, token);
        assertTrue(mdDetail.contains("<h1>导入标题</h1>"), mdDetail);
        assertTrue(mdDetail.contains("导入标题"), mdDetail);
        String htmlDetail = get("/api/knowledge/" + htmlId, token);
        assertTrue(htmlDetail.contains("<h1>HTML 导入</h1>"), htmlDetail);

        // 导出备份：JSON，含全部条目
        HttpResponse<String> export = rawGet("/api/knowledge/export", token);
        assertEquals(200, export.statusCode());
        assertTrue(export.body().contains("\"app\":\"iknow-youknow\""), export.body());
        assertTrue(export.body().contains("\"items\""), export.body());
    }

    private int ftsCount(String word) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM kb_knowledge WHERE search_tsv @@ plainto_tsquery('simple', ?)",
                Integer.class, word);
        return count == null ? 0 : count;
    }

    private String randomSuffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private String delete(String path, String token) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Authorization", "Bearer " + token)
                .DELETE()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString()).body();
    }

    private byte[] buildMultipart(String boundary, Map<String, String> fields, Map<String, byte[]> files)
            throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (Map.Entry<String, String> e : fields.entrySet()) {
            write(out, "--" + boundary + "\r\n");
            write(out, "Content-Disposition: form-data; name=\"" + e.getKey() + "\"\r\n\r\n");
            write(out, e.getValue() + "\r\n");
        }
        for (Map.Entry<String, byte[]> e : files.entrySet()) {
            write(out, "--" + boundary + "\r\n");
            write(out, "Content-Disposition: form-data; name=\"files\"; filename=\""
                    + e.getKey() + "\"\r\n");
            write(out, "Content-Type: application/octet-stream\r\n\r\n");
            out.write(e.getValue());
            write(out, "\r\n");
        }
        write(out, "--" + boundary + "--\r\n");
        return out.toByteArray();
    }

    private void write(ByteArrayOutputStream out, String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        out.write(bytes, 0, bytes.length);
    }
}
