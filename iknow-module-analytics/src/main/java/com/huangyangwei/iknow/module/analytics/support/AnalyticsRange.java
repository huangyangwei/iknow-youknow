package com.huangyangwei.iknow.module.analytics.support;

import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 仪表盘时间窗解析：range 支持 7d / 30d / 90d / all（空则默认 30d）。
 */
public final class AnalyticsRange {

    private static final long DEFAULT_DAYS = 30;

    private AnalyticsRange() {
    }

    public static LocalDateTime since(String range) {
        String value = range == null ? "" : range.trim();
        if (!StringUtils.hasText(value) || "all".equalsIgnoreCase(value)) {
            return DEFAULT_DAYS <= 0 ? null : LocalDateTime.now().minusDays(DEFAULT_DAYS);
        }
        String lower = value.toLowerCase();
        if (lower.endsWith("d")) {
            long days = parseDays(lower.substring(0, lower.length() - 1));
            return days <= 0 ? null : LocalDateTime.now().minusDays(days);
        }
        return LocalDateTime.now().minusDays(DEFAULT_DAYS);
    }

    private static long parseDays(String digits) {
        try {
            long days = Long.parseLong(digits.trim());
            return Math.min(days, 3650);
        } catch (NumberFormatException e) {
            return DEFAULT_DAYS;
        }
    }
}
