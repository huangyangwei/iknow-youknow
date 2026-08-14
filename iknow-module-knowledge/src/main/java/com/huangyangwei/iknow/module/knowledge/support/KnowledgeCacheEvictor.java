package com.huangyangwei.iknow.module.knowledge.support;

import com.huangyangwei.iknow.common.constant.Constants;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

/**
 * 知识域缓存失效：写操作后直接失效两级缓存（绕过注解代理，保证同 Service 自调用也生效）。
 */
@Component
public class KnowledgeCacheEvictor {

    private final CacheManager cacheManager;

    public KnowledgeCacheEvictor(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public void evictKnowledgeDetail(Long id) {
        evict(Constants.CACHE_KNOWLEDGE_DETAIL, id);
    }

    public void evictCategoryTree() {
        clear(Constants.CACHE_CATEGORY_TREE);
    }

    public void evictTagDict() {
        clear(Constants.CACHE_TAG_DICT);
    }

    private void evict(String cacheName, Object key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
        }
    }

    private void clear(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }
}
