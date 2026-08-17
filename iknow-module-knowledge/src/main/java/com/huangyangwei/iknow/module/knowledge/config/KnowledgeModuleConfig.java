package com.huangyangwei.iknow.module.knowledge.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 知识模块配置：开启定时调度（定时发布轮询）。
 */
@Configuration
@EnableScheduling
public class KnowledgeModuleConfig {
}
