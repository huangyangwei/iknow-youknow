package com.huangyangwei.iknow.infra.cache;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 两级缓存开关：启用 @Cacheable/@CacheEvict 等注解，CacheManager 为 L1 Caffeine + L2 Redis。
 */
@Configuration
@EnableCaching
public class TwoLevelCacheConfig {

    private static final String REDIS_KEY_PREFIX = "iknow:cache:";

    @Bean
    public TwoLevelCacheManager cacheManager(RedisTemplate<String, Object> redisTemplate, CacheProperties properties) {
        return new TwoLevelCacheManager(redisTemplate, properties, REDIS_KEY_PREFIX);
    }
}
