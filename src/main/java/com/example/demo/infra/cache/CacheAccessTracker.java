package com.example.demo.infra.cache;

import com.example.demo.app.service.CacheStatisticsService;
import com.example.demo.app.service.CacheStatisticsService.CacheAccessType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Utility to manually track cache access patterns when using service methods
 */
@Slf4j
@Component
public class CacheAccessTracker {

    private final CacheStatisticsService statisticsService;
    private final RedisTemplate<String, Object> redisTemplate;

    public CacheAccessTracker(CacheStatisticsService statisticsService,
                             RedisTemplate<String, Object> redisTemplate) {
        this.statisticsService = statisticsService;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Track cache access patterns for a given cache and key
     */
    public void trackAccess(String cacheName, Object key, Cache cache, boolean methodExecuted) {
        try {
            // Check L1 cache (Caffeine)
            boolean l1Hit = false;
            if (cache instanceof CaffeineCache caffeineCache) {
                com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();
                Object cachedValue = nativeCache.getIfPresent(key);
                l1Hit = (cachedValue != null);
            }

            if (l1Hit) {
                statisticsService.recordCacheAccess(cacheName, CacheAccessType.L1_HIT);
                return;
            }

            // L1 miss, check Redis
            boolean l2Hit = false;
            if (methodExecuted) {
                try {
                    String redisKey = cacheName + "::" + key;
                    l2Hit = Boolean.TRUE.equals(redisTemplate.hasKey(redisKey));
                } catch (Exception e) {
                    log.debug("Error checking Redis for key existence: {}", e.getMessage());
                }

                if (l2Hit) {
                    statisticsService.recordCacheAccess(cacheName, CacheAccessType.L1_MISS_L2_HIT);
                } else {
                    statisticsService.recordCacheAccess(cacheName, CacheAccessType.L1_MISS_L2_MISS);
                }
            }
        } catch (Exception e) {
            log.debug("Error tracking cache access: {}", e.getMessage());
        }
    }
}