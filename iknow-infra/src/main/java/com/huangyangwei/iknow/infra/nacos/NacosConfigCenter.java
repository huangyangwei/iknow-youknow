package com.huangyangwei.iknow.infra.nacos;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Nacos 配置中心接入（技术方案 R4 兜底方案：直接用 nacos-client SDK 拉取）。
 * 应用就绪后拉取一次配置并注册变更监听；拉取失败仅告警，不影响应用启动。
 * 配置内容暂存于 configSnapshot，供后续模块读取（如 LLM Key、缓存参数）。
 */
@Component
public class NacosConfigCenter {

    private static final Logger log = LoggerFactory.getLogger(NacosConfigCenter.class);

    private final NacosProperties properties;
    private volatile String configSnapshot;

    public NacosConfigCenter(NacosProperties properties) {
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        if (!properties.isEnabled()) {
            log.info("Nacos config center disabled (iknow.nacos.enabled=false)");
            return;
        }
        try {
            Properties props = new Properties();
            props.put("serverAddr", properties.getServerAddr());
            if (StringUtils.hasText(properties.getNamespace())) {
                props.put("namespace", properties.getNamespace());
            }
            ConfigService configService = NacosFactory.createConfigService(props);
            configSnapshot = configService.getConfigAndSignListener(
                    properties.getDataId(), properties.getGroup(), properties.getTimeoutMs(), new Listener() {
                        @Override
                        public Executor getExecutor() {
                            return Executors.newSingleThreadExecutor();
                        }

                        @Override
                        public void receiveConfigInfo(String configInfo) {
                            configSnapshot = configInfo;
                            log.info("Nacos config refreshed, dataId={}", properties.getDataId());
                        }
                    });
            log.info("Nacos config loaded, dataId={}, length={}",
                    properties.getDataId(), configSnapshot == null ? 0 : configSnapshot.length());
        } catch (Exception e) {
            log.warn("Nacos config load failed, app continues without external config: {}", e.getMessage());
        }
    }

    public String getConfigSnapshot() {
        return configSnapshot;
    }
}
