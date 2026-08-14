package com.huangyangwei.iknow.infra.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 两级缓存管理器：按缓存名懒创建 TwoLevelCache，L1 用 Caffeine。
 */
public class TwoLevelCacheManager implements CacheManager {

    private final ConcurrentHashMap<String, Cache> caches = new ConcurrentHashMap<>();
    private final RedisTemplate<String, Object> redis;
    private final CacheProperties properties;
    private final String redisKeyPrefix;

    public TwoLevelCacheManager(RedisTemplate<String, Object> redis, CacheProperties properties, String redisKeyPrefix) {
        this.redis = redis;
        this.properties = properties;
        this.redisKeyPrefix = redisKeyPrefix;
    }

    @Override
    public Cache getCache(String name) {
        return caches.computeIfAbsent(name, this::buildCache);
    }

    @Override
    public Collection<String> getCacheNames() {
        return Set.copyOf(caches.keySet());
    }

    private TwoLevelCache buildCache(String name) {
        Caffeine<Object, Object> builder = Caffeine.newBuilder()
                .maximumSize(properties.getL1MaxSize())
                .expireAfterWrite(properties.getL1Ttl());
        return new TwoLevelCache(name, builder.build(), redis, properties.getL2Ttl(), redisKeyPrefix);
    }
}
