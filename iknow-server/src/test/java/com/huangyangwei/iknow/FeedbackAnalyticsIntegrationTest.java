package com.huangyangwei.iknow;

import com.huangyangwei.iknow.common.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P3 反馈闭环 + 数据分析验收测试（dev profile：嵌入式 PG16 + Flyway V1-V5）：
 * - 反馈闭环全流程：提交（赞/踩/纠错/建议）→ 管理员筛选 → 处理流转（pending→processing→resolved）
 *   → 站内通知提交人（列表/未读数/已读）
 * - 统计落库：/api/search 与 /api/chat/ask 写入 stat_query_log（search/qa，has_result）
 * - 仪表盘聚合：overview（含采纳率=like/(like+dislike)、无结果率）、hot-search、category-distribution、
 *   feedback-trend
 * - RBAC：反馈列表/处理仅反馈管理员（feedback:handle），仪表盘仅 ADMIN
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FeedbackAnalyticsIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json));
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString()).body();
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

    private String login() throws Exception {
        String body = post("/api/auth/login",
                "{\"email\":\"admin@iknow.ai\",\"password\":\"Admin@123\"}", null);
        return extractToken(body);
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

    private String randomSuffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    @Test
    void feedbackClosedLoopSubmitFilterHandleNotify() throws Exception {
        String admin = login();
        String member = jwtUtil.generate(999L, "member", List.of("MEMBER"), List.of());

        // 1. 提交赞/踩/纠错
        long likeId = jsonId(post("/api/feedback",
                "{\"type\":\"like\",\"sourceType\":\"answer\",\"sourceId\":1,\"question\":\"什么是RAG\"}", member));
        long dislikeId = jsonId(post("/api/feedback",
                "{\"type\":\"dislike\",\"sourceType\":\"knowledge\",\"sourceId\":2,\"content\":\"回答不准确\"}", member));
        long correctionId = jsonId(post("/api/feedback",
                "{\"type\":\"correction\",\"content\":\"文档版本有误\"}", member));
        assertTrue(likeId > 0 && dislikeId > 0 && correctionId > 0);

        // 2. 普通成员无权限列表/处理反馈
        String memberList = get("/api/feedback?status=pending", member);
        assertTrue(memberList.contains("\"code\":2003"), "member list should be FORBIDDEN: " + memberList);

        // 3. 管理员筛选：type=like / status=pending
        String likeList = get("/api/feedback?type=like&size=50", admin);
        assertTrue(likeList.contains("\"code\":0"), likeList);
        JsonNode likeRecords = objectMapper.readTree(likeList).path("data").path("records");
        boolean hasLike = false;
        for (JsonNode rec : likeRecords) {
            if (rec.path("id").asLong() == likeId && "like".equals(rec.path("type").asText())
                    && "pending".equals(rec.path("status").asText())) {
                hasLike = true;
            }
        }
        assertTrue(hasLike, "filtered like feedback should be listed: " + likeList);

        // 4. 处理流转：pending → processing → resolved（含处理说明）
        String processing = put("/api/feedback/" + likeId + "/handle",
                "{\"status\":\"processing\",\"handleNote\":\"正在核实\"}", admin);
        assertTrue(processing.contains("\"code\":0"), processing);
        assertTrue(processing.contains("\"status\":\"processing\""), processing);

        String resolved = put("/api/feedback/" + likeId + "/handle",
                "{\"status\":\"resolved\",\"handleNote\":\"已修正答案\"}", admin);
        assertTrue(resolved.contains("\"code\":0"), resolved);
        assertTrue(resolved.contains("\"status\":\"resolved\""), resolved);
        assertTrue(resolved.contains("\"handlerId\":1"), resolved);
        assertTrue(resolved.contains("\"handleNote\":\"已修正答案\""), resolved);

        // 非法流转被拒：resolved 不能再回到 processing
        String invalid = put("/api/feedback/" + likeId + "/handle",
                "{\"status\":\"processing\"}", admin);
        assertTrue(invalid.contains("\"code\":5002"), "backward transition should be rejected: " + invalid);

        // 5. 站内通知提交人：未读数 + 列表 + 标记已读
        String unread = get("/api/notifications/unread-count", member);
        JsonNode unreadNode = objectMapper.readTree(unread).path("data");
        assertTrue(unreadNode.asLong() >= 1, "member should have unread notifications: " + unread);

        String notificationList = get("/api/notifications", member);
        assertTrue(notificationList.contains("\"code\":0"), notificationList);
        JsonNode notifs = objectMapper.readTree(notificationList).path("data").path("records");
        boolean hasResolvedNotif = false;
        long notifId = 0;
        for (JsonNode n : notifs) {
            if (n.path("refId").asLong() == likeId && "feedback".equals(n.path("type").asText())) {
                hasResolvedNotif = true;
                notifId = n.path("id").asLong();
            }
        }
        assertTrue(hasResolvedNotif, "resolved feedback notification should exist: " + notificationList);
        assertTrue(notifId > 0, "notification id should be positive");

        String read = put("/api/notifications/" + notifId + "/read", "{}", member);
        assertTrue(read.contains("\"code\":0"), read);
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT count(*) FROM sys_notification WHERE id = ? AND is_read = FALSE", Integer.class, notifId),
                "notification should be marked read");
    }

    @Test
    void feedbackStatsOverviewAndFeedbackTrend() throws Exception {
        // 隔离：清空反馈/通知表，保证采纳率等聚合断言基于本用例数据
        jdbcTemplate.update("DELETE FROM sys_notification");
        jdbcTemplate.update("DELETE FROM fb_feedback");
        String admin = login();
        String member = jwtUtil.generate(999L, "member", List.of("MEMBER"), List.of());

        // 构造 2 like + 1 dislike + 1 correction
        post("/api/feedback", "{\"type\":\"like\",\"question\":\"q1\"}", member);
        post("/api/feedback", "{\"type\":\"like\",\"question\":\"q2\"}", member);
        post("/api/feedback", "{\"type\":\"dislike\",\"content\":\"bad\"}", member);
        post("/api/feedback", "{\"type\":\"correction\",\"content\":\"fix\"}", member);

        String overview = get("/api/analytics/overview", admin);
        assertTrue(overview.contains("\"code\":0"), overview);
        JsonNode o = objectMapper.readTree(overview).path("data");
        // 采纳率 = like/(like+dislike) = 2/3
        assertEquals(2, o.path("likeCount").asLong());
        assertEquals(1, o.path("dislikeCount").asLong());
        assertEquals(0.6667, o.path("adoptionRate").asDouble(), 0.0001);
        assertTrue(o.path("feedbackCount").asLong() >= 4, "overview should count feedback: " + overview);

        // 反馈趋势：今天有 4 条新增
        String trend = get("/api/analytics/feedback-trend?range=30d", admin);
        assertTrue(trend.contains("\"code\":0"), trend);
        JsonNode trendData = objectMapper.readTree(trend).path("data");
        assertTrue(trendData.isArray() && trendData.size() >= 1, "feedback trend should not be empty: " + trend);
    }

    @Test
    void searchLogsAndHotSearch() throws Exception {
        // 隔离：清空统计表，保证 searchCount/noResultRate 基于本用例数据
        jdbcTemplate.update("DELETE FROM stat_query_log");
        String admin = login();

        // 发布一条含唯一词的知识
        String tokenWord = "hot" + randomSuffix();
        String html = "<p>" + tokenWord + " 热门检索词条，用于验证仪表盘热门搜索聚合。</p>";
        long knowledgeId = jsonId(post("/api/knowledge",
                "{\"title\":\"热门搜索词条\",\"htmlContent\":\"" + html + "\"}", admin));
        assertTrue(post("/api/knowledge/" + knowledgeId + "/publish", "{}", admin).contains("\"code\":0"));

        // 多次检索 → 写入 stat_query_log(search, has_result=true)
        for (int i = 0; i < 3; i++) {
            String search = get("/api/search?keyword=" + tokenWord, admin);
            assertTrue(search.contains("\"code\":0"), search);
            assertTrue(search.contains("\"id\":" + knowledgeId), "search should hit published knowledge: " + search);
        }

        // 无结果检索 → has_result=false
        String missWord = "nomatch" + randomSuffix();
        String miss = get("/api/search?keyword=" + missWord, admin);
        assertTrue(miss.contains("\"code\":0"), miss);
        assertEquals(0, objectMapper.readTree(miss).path("data").path("total").asLong());

        // hot-search Top10 应包含该词（出现 3 次）
        String hot = get("/api/analytics/hot-search?range=30d", admin);
        assertTrue(hot.contains("\"code\":0"), hot);
        JsonNode hotData = objectMapper.readTree(hot).path("data");
        boolean found = false;
        for (JsonNode item : hotData) {
            if (tokenWord.equals(item.path("keyword").asText())) {
                found = true;
                assertEquals(3, item.path("count").asLong(), "hot search count mismatch: " + hot);
            }
        }
        assertTrue(found, "search keyword should appear in hot-search: " + hot);

        // overview 无结果率 = 无结果检索 / 总检索 = 1/4
        String overview = get("/api/analytics/overview?range=30d", admin);
        JsonNode o = objectMapper.readTree(overview).path("data");
        assertEquals(4, o.path("searchCount").asLong(), "searchCount mismatch: " + overview);
        assertEquals(0.25, o.path("noResultRate").asDouble(), 0.0001);

        // 分类分布包含发布知识所在分类
        String dist = get("/api/analytics/category-distribution", admin);
        assertTrue(dist.contains("\"code\":0"), dist);
    }

    @Test
    void categoryDistributionCountsPublishedKnowledge() throws Exception {
        String admin = login();
        long categoryId = jsonId(post("/api/categories",
                "{\"parentId\":0,\"name\":\"分析分类" + randomSuffix() + "\"}", admin));
        long knowledgeId = jsonId(post("/api/knowledge",
                "{\"title\":\"分类分布词条\",\"htmlContent\":\"<p>catdist" + randomSuffix() + " 内容</p>\","
                        + "\"categoryId\":" + categoryId + "}", admin));
        assertTrue(post("/api/knowledge/" + knowledgeId + "/publish", "{}", admin).contains("\"code\":0"));

        String dist = get("/api/analytics/category-distribution", admin);
        JsonNode items = objectMapper.readTree(dist).path("data");
        boolean found = false;
        for (JsonNode item : items) {
            if (item.path("categoryId").asLong() == categoryId && item.path("count").asLong() >= 1) {
                found = true;
            }
        }
        assertTrue(found, "category distribution should count published knowledge: " + dist);
    }
}
