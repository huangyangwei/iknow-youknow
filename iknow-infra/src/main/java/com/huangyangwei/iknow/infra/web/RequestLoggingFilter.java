package com.huangyangwei.iknow.infra.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * 请求/响应日志过滤器：记录所有 /api 接口的 method、URI、请求体、响应状态、响应体与耗时。
 * <p>
 * 注意事项：
 * <ul>
 *   <li>排除了 SSE 流式端点（/api/chat/ask），避免缓存流式响应导致内存溢出</li>
 *   <li>请求体超过 4KB 时仅记录前 4KB，防止大文件上传撑爆日志</li>
 *   <li>依赖 TraceIdFilter 先执行（Order = HIGHEST_PRECEDENCE），日志中自动携带 traceId</li>
 * </ul>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    /** SSE 或流式端点，不缓存响应体 */
    private static final Set<String> SSE_URIS = Set.of("/api/chat/ask");

    /** 请求/响应体最大记录长度 */
    private static final int MAX_BODY_LENGTH = 4096;

    /** 请求体缓存容量（字节），与 MAX_BODY_LENGTH 对齐 */
    private static final int CONTENT_CACHE_LIMIT = MAX_BODY_LENGTH;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();
        boolean isSse = SSE_URIS.contains(uri);

        // 对非 SSE 请求包装 request/response 以缓存 body
        if (!isSse && isApiRequest(uri)) {
            ContentCachingRequestWrapper cachingRequest = new ContentCachingRequestWrapper(request, CONTENT_CACHE_LIMIT);
            ContentCachingResponseWrapper cachingResponse = new ContentCachingResponseWrapper(response);

            long start = System.currentTimeMillis();
            try {
                filterChain.doFilter(cachingRequest, cachingResponse);
            } finally {
                long cost = System.currentTimeMillis() - start;
                logRequest(cachingRequest);
                logResponse(cachingResponse, cost);
                cachingResponse.copyBodyToResponse();
            }
        } else {
            // SSE 或非 /api 请求：仅记录请求行，不缓存 body
            if (isApiRequest(uri)) {
                long start = System.currentTimeMillis();
                try {
                    filterChain.doFilter(request, response);
                } finally {
                    long cost = System.currentTimeMillis() - start;
                    log.info(">> {} {} | cost={}ms | status={} | [SSE, body skipped]",
                            request.getMethod(), uri, cost, response.getStatus());
                }
            } else {
                filterChain.doFilter(request, response);
            }
        }
    }

    private boolean isApiRequest(String uri) {
        return uri.startsWith("/api/") || uri.startsWith("/actuator/");
    }

    private void logRequest(ContentCachingRequestWrapper request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        String body = truncate(new String(request.getContentAsByteArray(), StandardCharsets.UTF_8));

        if (queryString != null) {
            log.info(">> {} {}?{} | body={}", method, uri, queryString, body);
        } else {
            log.info(">> {} {} | body={}", method, uri, body);
        }
    }

    private void logResponse(ContentCachingResponseWrapper response, long cost) {
        int status = response.getStatus();
        String body = truncate(new String(response.getContentAsByteArray(), StandardCharsets.UTF_8));
        log.info("<< status={} | cost={}ms | body={}", status, cost, body);
    }

    private String truncate(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        if (text.length() > MAX_BODY_LENGTH) {
            return text.substring(0, MAX_BODY_LENGTH) + "...[truncated]";
        }
        return text;
    }
}
