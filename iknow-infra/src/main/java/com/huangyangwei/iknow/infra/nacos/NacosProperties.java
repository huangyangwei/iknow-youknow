package com.huangyangwei.iknow.infra.nacos;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Nacos 配置中心参数（iknow.nacos.*）。默认关闭，未配置 Nacos 时应用照常启动。
 */
@Component
@ConfigurationProperties(prefix = "iknow.nacos")
public class NacosProperties {

    private boolean enabled = false;
    private String serverAddr = "127.0.0.1:8848";
    private String namespace = "";
    private String dataId = "iknow-youknow";
    private String group = "DEFAULT_GROUP";
    private long timeoutMs = 3000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getServerAddr() {
        return serverAddr;
    }

    public void setServerAddr(String serverAddr) {
        this.serverAddr = serverAddr;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getDataId() {
        return dataId;
    }

    public void setDataId(String dataId) {
        this.dataId = dataId;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
}
