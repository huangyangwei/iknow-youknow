package com.huangyangwei.iknow.infra.web;

import com.huangyangwei.iknow.common.constant.Constants;
import com.huangyangwei.iknow.common.util.TraceIdUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * TraceId 过滤器：优先复用请求头 X-Request-Id，否则生成新 ID，
 * 写入 MDC 贯穿整条调用链，响应后可读。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = request.getHeader(Constants.REQUEST_ID_HEADER);
        if (requestId != null && !requestId.isBlank()) {
            MDC.put(Constants.TRACE_ID, requestId);
        } else {
            TraceIdUtil.setTraceId();
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(Constants.TRACE_ID);
        }
    }
}
