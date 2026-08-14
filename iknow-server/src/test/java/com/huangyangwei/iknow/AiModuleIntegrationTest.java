package com.huangyangwei.iknow;

import com.huangyangwei.iknow.common.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P2 AI 模块验收测试（dev profile：嵌入式 PG16 + Flyway V1/V2/V3）：
 * - GET /api/models：沙箱无外部密钥时仍暴露本地确定性模型；
 * - POST /api/chat/ask：SSE 流式返回 start → delta* → done，done 携带模型/置信度/引用；
 * - 引用来源来自 PG 全文检索（published 强制过滤，无 pgvector 时向量通道降级为空）；
 * - 问答会话/消息持久化：/api/chat/sessions + /api/chat/sessions/{id}/messages。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AiModuleIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

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

    private List<JsonNode> parseSse(String body) throws Exception {
        List<JsonNode> events = new ArrayList<>();
        for (String line : body.split("\\R")) {
            if (line.startsWith("data:")) {
                events.add(objectMapper.readTree(line.substring(5).trim()));
            }
        }
        return events;
    }

    @Test
    void modelsEndpointListsAvailableModels() throws Exception {
        String token = login();
        String body = get("/api/models", token);
        assertTrue(body.contains("\"code\":0"), body);
        assertTrue(body.contains("\"key\":\"deterministic\""), "sandbox must expose deterministic model: " + body);
        assertTrue(body.contains("本地确定性模型"), body);

        // C1 回归守卫：密钥未设置的 provider 不得注册/列出。断言按测试环境实际密钥情况
        // 动态判定，无论环境是否有密钥都成立——有密钥的 provider 不参与该断言。
        assertUnlistedWhenKeyUnset(body, "ANTHROPIC_AUTH_TOKEN", "claude-opus-5");
        assertUnlistedWhenKeyUnset(body, "OPENAI_API_KEY", "gpt-4o");
        assertUnlistedWhenKeyUnset(body, "GEMINI_API_KEY", "gemini-2.5-pro");
        assertUnlistedWhenKeyUnset(body, "DEEPSEEK_API_KEY", "deepseek-v3");
    }

    private void assertUnlistedWhenKeyUnset(String body, String envVar, String modelKey) {
        String value = System.getenv(envVar);
        if (value != null && !value.isBlank()) {
            return; // 环境已配置该密钥：该 provider 允许注册，跳过断言
        }
        assertFalse(body.contains("\"key\":\"" + modelKey + "\""),
                modelKey + " must not be listed when " + envVar + " is unset: " + body);
    }

    @Test
    void chatAskStreamsSseWithCitationsAndPersistsSession() throws Exception {
        String token = login();

        // 发布一条含唯一词条的知识：问题词=“什么是ragtokXXX”（simple 分词下为单个词元，保证 FTS 命中）
        String keyword = "ragtok" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String question = "什么是" + keyword;
        String html = "<p>" + question + "，这是企业知识库 RAG 检索演示词条。" + keyword
                + " 仅用于验证混合检索与引用链路。</p>";
        long knowledgeId = jsonId(post("/api/knowledge",
                "{\"title\":\"RAG 演示词条\",\"htmlContent\":\"" + html + "\"}", token));
        String publishBody = post("/api/knowledge/" + knowledgeId + "/publish", "{}", token);
        assertTrue(publishBody.contains("\"code\":0"), publishBody);

        // 流式提问（显式指定本地确定性模型）
        HttpRequest askRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/chat/ask"))
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"model\":\"deterministic\",\"question\":\"" + question + "\"}"))
                .build();
        String sseBody = client.send(askRequest, HttpResponse.BodyHandlers.ofString()).body();
        assertTrue(sseBody.contains("data:"), "expected SSE events: " + sseBody);

        List<JsonNode> events = parseSse(sseBody);
        assertTrue(events.size() >= 3, "expected start+delta+done events, got " + events.size());

        JsonNode start = events.get(0);
        assertEquals("start", start.path("type").asText());
        long sessionId = start.path("sessionId").asLong();
        assertTrue(sessionId > 0, "start event should carry sessionId");

        boolean hasDelta = false;
        for (JsonNode event : events) {
            if ("delta".equals(event.path("type").asText())) {
                hasDelta = true;
                assertTrue(event.path("content").asText().length() > 0, "delta content must not be empty");
            }
        }
        assertTrue(hasDelta, "at least one delta event expected");

        JsonNode done = events.get(events.size() - 1);
        assertEquals("done", done.path("type").asText(), "last event should be done");
        assertEquals("deterministic", done.path("model").asText());
        assertEquals("本地确定性模型", done.path("modelName").asText());
        assertEquals(sessionId, done.path("sessionId").asLong());
        assertTrue(done.path("answer").asText().contains(question), "answer should echo the question");
        assertTrue(done.has("confidenceScore"), "done should carry confidenceScore");
        assertTrue(done.path("sources").isArray() && done.path("sources").size() > 0,
                "FTS citation expected in done.sources: " + sseBody);
        JsonNode firstSource = done.path("sources").get(0);
        assertEquals(knowledgeId, firstSource.path("knowledgeId").asLong());
        assertEquals("RAG 演示词条", firstSource.path("title").asText());

        // 会话已持久化且属于当前用户
        String sessionsBody = get("/api/chat/sessions", token);
        assertTrue(sessionsBody.contains("\"code\":0"), sessionsBody);
        assertTrue(sessionsBody.contains("\"id\":" + sessionId), "session should be listed: " + sessionsBody);

        // 消息已持久化：user + assistant（assistant 携带 model/confidence/sources）
        String messagesBody = get("/api/chat/sessions/" + sessionId + "/messages", token);
        assertTrue(messagesBody.contains("\"code\":0"), messagesBody);
        JsonNode messages = objectMapper.readTree(messagesBody).path("data");
        assertTrue(messages.isArray() && messages.size() >= 2, "user+assistant messages expected: " + messagesBody);

        boolean sawUser = false;
        boolean sawAssistant = false;
        for (JsonNode message : messages) {
            if ("user".equals(message.path("role").asText())) {
                sawUser = true;
                assertEquals(question, message.path("content").asText());
            }
            if ("assistant".equals(message.path("role").asText())) {
                sawAssistant = true;
                assertEquals("deterministic", message.path("model").asText());
                assertTrue(message.path("confidence").asText().length() > 0, "assistant must carry confidence");
                assertTrue(message.path("sources").asText().contains("\"knowledgeId\":" + knowledgeId),
                        "assistant sources should persist citation: " + message.path("sources").asText());
            }
        }
        assertTrue(sawUser && sawAssistant, "both user and assistant messages must be persisted");
    }

    @Test
    void chatAskWithUnknownModelRejects() throws Exception {
        String token = login();
        String body = post("/api/chat/ask",
                "{\"model\":\"no-such-model\",\"question\":\"你好\"}", token);
        assertTrue(body.contains("未知模型"), "expected unknown-model error: " + body);
    }

    @Test
    void defaultModelWithoutExplicitChoiceCompletesWithRegisteredModel() throws Exception {
        // 未显式指定模型时：默认模型注册（有密钥）则使用之，否则回退到首个可用模型。
        // 无论哪种环境，SSE 流都必须以 done 事件结束并标注实际使用的模型。
        String token = login();
        JsonNode models = objectMapper.readTree(get("/api/models", token)).path("data");
        assertTrue(models.isArray() && models.size() >= 1, "at least one model should be available");

        HttpRequest askRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/chat/ask"))
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString("{\"question\":\"默认模型回退测试\"}"))
                .build();
        String sseBody = client.send(askRequest, HttpResponse.BodyHandlers.ofString()).body();
        List<JsonNode> events = parseSse(sseBody);
        assertNotNull(events, "expected SSE events");
        JsonNode done = events.get(events.size() - 1);
        assertEquals("done", done.path("type").asText(), "expected done event: " + sseBody);
        String usedModel = done.path("model").asText();
        assertTrue(usedModel.length() > 0, "done must label the model used");
        boolean registered = false;
        for (JsonNode model : models) {
            if (model.path("key").asText().equals(usedModel)) {
                registered = true;
                break;
            }
        }
        assertTrue(registered, "used model must be one of the registered models: " + usedModel);
    }
}
