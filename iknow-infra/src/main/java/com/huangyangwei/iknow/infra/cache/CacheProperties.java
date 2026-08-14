package com.huangyangwei.iknow.infra.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 两级缓存参数（iknow.cache.*）。
 */
@Component
@ConfigurationProperties(prefix = "iknow.cache")
public class CacheProperties {

    /** L1 Caffeine 本地缓存 TTL */
    private Duration l1Ttl = Duration.ofMinutes(5);
    /** L1 最大条目数 */
    private long l1MaxSize = 1000;
    /** L2 Redis 缓存 TTL */
    private Duration l2Ttl = Duration.ofMinutes(30);

    public Duration getL1Ttl() {
        return l1Ttl;
    }

    public void setL1Ttl(Duration l1Ttl) {
        this.l1Ttl = l1Ttl;
    }

    public long getL1MaxSize() {
        return l1MaxSize;
    }

    public void setL1MaxSize(long l1MaxSize) {
        this.l1MaxSize = l1MaxSize;
    }

    public Duration getL2Ttl() {
        return l2Ttl;
    }

    public void setL2Ttl(Duration l2Ttl) {
        this.l2Ttl = l2Ttl;
    }
}
