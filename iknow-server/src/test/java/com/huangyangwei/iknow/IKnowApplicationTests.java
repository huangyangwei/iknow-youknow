package com.huangyangwei.iknow;

import com.huangyangwei.iknow.common.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P0 验收测试（dev profile：嵌入式 PG16 + Flyway 迁移）：
 * 1) 应用可启动、/api/auth/login 返回 JWT；
 * 2) /auth/me 返回用户+角色；
 * 3) RBAC 注解鉴权：管理员放行、非管理员 2003；
 * 4) 未登录访问受保护接口返回 401；
 * 5) 错误密码返回 2004。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IKnowApplicationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private JwtUtil jwtUtil;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private String post(String path, String json) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
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

    private String login() throws Exception {
        String body = post("/api/auth/login", "{\"email\":\"admin@iknow.ai\",\"password\":\"Admin@123\"}");
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

    @Test
    void contextLoads() {
        assertTrue(true);
    }

    @Test
    void loginReturnsJwtWithRolesAndMeReturnsUser() throws Exception {
        String loginBody = post("/api/auth/login",
                "{\"email\":\"admin@iknow.ai\",\"password\":\"Admin@123\"}");
        assertTrue(loginBody.contains("\"code\":0"), "login should succeed: " + loginBody);
        assertTrue(loginBody.contains("\"tokenType\":\"Bearer\""), loginBody);
        assertTrue(loginBody.contains("\"roles\":[\"ADMIN\"]"), loginBody);
        String token = extractToken(loginBody);

        String meBody = get("/api/auth/me", token);
        assertTrue(meBody.contains("\"code\":0"), meBody);
        assertTrue(meBody.contains("\"username\":\"admin\""), meBody);
        assertTrue(meBody.contains("\"email\":\"admin@iknow.ai\""), meBody);
        assertTrue(meBody.contains("\"roles\":[\"ADMIN\"]"), meBody);
    }

    @Test
    void rbacAllowsAdmin() throws Exception {
        String token = login();
        String body = get("/api/auth/admin/ping", token);
        assertTrue(body.contains("\"code\":0"), body);
        assertTrue(body.contains("\"data\":\"admin:ok\""), body);
    }

    @Test
    void rbacRejectsNonAdmin() throws Exception {
        String memberToken = jwtUtil.generate(999L, "member", List.of("MEMBER"), List.of());
        String body = get("/api/auth/admin/ping", memberToken);
        assertTrue(body.contains("\"code\":2003"), "expected FORBIDDEN: " + body);
    }

    @Test
    void unauthenticatedRequestReturns401() throws Exception {
        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:" + port + "/api/auth/me"))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(401, response.statusCode());
        assertTrue(response.body().contains("\"code\":2001"), response.body());
    }

    @Test
    void wrongPasswordReturnsLoginFailed() throws Exception {
        String body = post("/api/auth/login", "{\"email\":\"admin@iknow.ai\",\"password\":\"WrongPass\"}");
        assertTrue(body.contains("\"code\":2004"), "expected LOGIN_FAILED: " + body);
    }

    @Test
    void traceIdAlwaysPresent() throws Exception {
        String body = post("/api/auth/login",
                "{\"email\":\"admin@iknow.ai\",\"password\":\"Admin@123\"}");
        assertNotNull(extractTraceId(body), body);
    }

    private String extractTraceId(String body) {
        String marker = "\"traceId\":\"";
        int start = body.indexOf(marker);
        if (start < 0) {
            return null;
        }
        start += marker.length();
        int end = body.indexOf('"', start);
        return body.substring(start, end);
    }
}
