package com.huangyangwei.iknow.infra.cache;

import com.github.benmanes.caffeine.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * 两级缓存：先查本地 Caffeine（L1），未命中再查 Redis（L2），
 * L2 命中后回填 L1。写操作双写、失效双删。
 * Redis（L2）不可用时降级为仅 L1：缓存故障不阻断业务请求，仅 WARN 日志。
 */
public class TwoLevelCache implements org.springframework.cache.Cache {

    private static final Logger log = LoggerFactory.getLogger(TwoLevelCache.class);

    private final String name;
    private final Cache<String, Object> l1;
    private final RedisTemplate<String, Object> redis;
    private final Duration l2Ttl;
    private final String redisKeyPrefix;

    public TwoLevelCache(String name, Cache<String, Object> l1, RedisTemplate<String, Object> redis,
                         Duration l2Ttl, String redisKeyPrefix) {
        this.name = name;
        this.l1 = l1;
        this.redis = redis;
        this.l2Ttl = l2Ttl;
        this.redisKeyPrefix = redisKeyPrefix;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getNativeCache() {
        return l1;
    }

    @Override
    public ValueWrapper get(Object key) {
        String cacheKey = redisKey(key);
        Object cached = l1.getIfPresent(cacheKey);
        if (cached != null) {
            return toValueWrapper(cached);
        }
        Object value = getFromRedis(cacheKey);
        if (value != null) {
            l1.put(cacheKey, value);
            return toValueWrapper(value);
        }
        return null;
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        ValueWrapper wrapper = get(key);
        if (wrapper == null) {
            return null;
        }
        Object value = wrapper.get();
        return type != null && value != null && type.isInstance(value) ? type.cast(value) : null;
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        ValueWrapper wrapper = get(key);
        if (wrapper != null) {
            @SuppressWarnings("unchecked")
            T value = (T) wrapper.get();
            if (value != null) {
                return value;
            }
        }
        try {
            T value = valueLoader.call();
            if (value != null) {
                put(key, value);
            }
            return value;
        } catch (Exception e) {
            throw new ValueRetrievalException(key, valueLoader, e);
        }
    }

    @Override
    public void put(Object key, Object value) {
        String cacheKey = redisKey(key);
        l1.put(cacheKey, value);
        try {
            redis.opsForValue().set(cacheKey, value, l2Ttl);
        } catch (Exception e) {
            log.warn("two-level-cache: L2 write failed for cache={}, key={}, degrade to L1 only", name, key, e);
        }
    }

    @Override
    public void evict(Object key) {
        String cacheKey = redisKey(key);
        l1.invalidate(cacheKey);
        try {
            redis.delete(cacheKey);
        } catch (Exception e) {
            log.warn("two-level-cache: L2 evict failed for cache={}, key={}", name, key, e);
        }
    }

    @Override
    public void clear() {
        l1.invalidateAll();
        try {
            Set<String> keys = redis.keys(redisKeyPrefix + name + ":*");
            if (keys != null && !keys.isEmpty()) {
                redis.delete(keys);
            }
        } catch (Exception e) {
            log.warn("two-level-cache: L2 clear failed for cache={}", name, e);
        }
    }

    private Object getFromRedis(String cacheKey) {
        try {
            return redis.opsForValue().get(cacheKey);
        } catch (Exception e) {
            log.warn("two-level-cache: L2 read failed for cache={}, key={}", name, cacheKey, e);
            return null;
        }
    }

    private String redisKey(Object key) {
        return redisKeyPrefix + name + ":" + key;
    }

    private ValueWrapper toValueWrapper(Object value) {
        return value == null ? null : new SimpleValueWrapper(value);
    }
}
