package com.huangyangwei.iknow.module.ai.model;

/**
 * 对外暴露的模型元信息（GET /api/models）。
 */
public record ModelInfo(String key, String name, String description) {
}
