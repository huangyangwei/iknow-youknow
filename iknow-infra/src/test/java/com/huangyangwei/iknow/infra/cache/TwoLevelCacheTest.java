package com.huangyangwei.iknow.infra.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 两级缓存行为单测：L1 命中、L2 回填、双写双删、Redis 不可用降级仅 L1。
 */
class TwoLevelCacheTest {

    private static final String PREFIX = "iknow:cache:";

    @SuppressWarnings("unchecked")
    private final RedisTemplate<String, Object> redis = mock(RedisTemplate.class);

    private TwoLevelCache cache(String name) {
        return new TwoLevelCache(name,
                Caffeine.newBuilder().maximumSize(100).build(),
                redis, Duration.ofMinutes(30), PREFIX);
    }

    @Test
    void l1HitDoesNotTouchRedisAgain() {
        when(redis.opsForValue()).thenReturn(mock(ValueOperations.class));
        TwoLevelCache cache = cache("demo");
        cache.put("a", "v1");
        verify(redis, times(1)).opsForValue();

        Cache.ValueWrapper wrapper = cache.get("a");
        assertThat(wrapper.get()).isEqualTo("v1");
        verify(redis, times(1)).opsForValue();
    }

    @Test
    void l2HitBackfillsL1() {
        @SuppressWarnings("unchecked")
        ValueOperations<String, Object> vops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(vops);
        when(vops.get(PREFIX + "demo:key1")).thenReturn("redis-value");

        TwoLevelCache cache = cache("demo");
        assertThat(cache.get("key1").get()).isEqualTo("redis-value");
        assertThat(cache.get("key1").get()).isEqualTo("redis-value");
        verify(redis, times(1)).opsForValue();
    }

    @Test
    void redisDownDegradesToL1Only() {
        when(redis.opsForValue()).thenThrow(new RuntimeException("connection refused"));
        TwoLevelCache cache = cache("demo");
        cache.put("k", "local");
        assertThat(cache.get("k").get()).isEqualTo("local");
        assertThat(cache.get("missing")).isNull();
    }

    @Test
    void putWritesBothLevelsAndEvictClearsBoth() {
        @SuppressWarnings("unchecked")
        ValueOperations<String, Object> vops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(vops);
        when(redis.delete(anyString())).thenReturn(true);

        TwoLevelCache cache = cache("demo");
        cache.put("k", "v");
        verify(vops).set(eq(PREFIX + "demo:k"), eq("v"), eq(Duration.ofMinutes(30)));

        cache.evict("k");
        verify(redis).delete(PREFIX + "demo:k");
        assertThat(cache.get("k")).isNull();
    }

    @Test
    void evictSurvivesRedisFailure() {
        when(redis.delete(anyString())).thenThrow(new RuntimeException("down"));
        when(redis.opsForValue()).thenReturn(mock(ValueOperations.class));
        TwoLevelCache cache = cache("demo");
        cache.put("k", "v");
        cache.evict("k");
        assertThat(cache.get("k")).isNull();
        verify(redis, never()).keys(anyString());
    }
}
