package com.huangyangwei.iknow.common.util;

import com.huangyangwei.iknow.common.constant.Constants;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * TraceId 工具：请求链路 ID，写入 SLF4J MDC，贯穿日志与统一响应体。
 */
public final class TraceIdUtil {

    private TraceIdUtil() {
    }

    public static String generate() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String getTraceId() {
        return MDC.get(Constants.TRACE_ID);
    }

    /**
     * 获取当前链路 ID；若尚未生成则先生成并写入 MDC，保证响应体 traceId 永不为空。
     */
    public static String getOrCreate() {
        String traceId = MDC.get(Constants.TRACE_ID);
        if (traceId == null || traceId.isBlank()) {
            traceId = setTraceId();
        }
        return traceId;
    }

    public static String setTraceId() {
        String traceId = generate();
        MDC.put(Constants.TRACE_ID, traceId);
        return traceId;
    }

    public static void clear() {
        MDC.remove(Constants.TRACE_ID);
    }
}
