package com.example.demo.infra.cache;

import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.example.demo.app.service.CacheStatisticsService;
import com.example.demo.app.service.CacheStatisticsService.CacheAccessType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility to manually track cache access patterns when using service methods.
 * Uses modern Java practices and optimized for performance.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheAccessTracker {

    private final CacheStatisticsService statisticsService;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Track cache access patterns for a given cache and key using modern Java patterns
     */
    public void trackAccess(String cacheName, Object key, Cache cache, boolean methodExecuted) {
        try {
            // Check L1 cache (Caffeine) using pattern matching
            var l1Hit = false;
            if (cache instanceof CaffeineCache caffeineCache) {
                var nativeCache = caffeineCache.getNativeCache();
                var cachedValue = nativeCache.getIfPresent(key);
                l1Hit = cachedValue != null;
            }

            if (l1Hit) {
                statisticsService.recordCacheAccess(cacheName, CacheAccessType.L1_HIT);
                return;
            }

            // L1 miss, check Redis with modern error handling
            if (methodExecuted) {
                var l2Hit = checkRedisKey(cacheName, key);
                var accessType = l2Hit ? CacheAccessType.L1_MISS_L2_HIT : CacheAccessType.L1_MISS_L2_MISS;
                statisticsService.recordCacheAccess(cacheName, accessType);
            }
        } catch (Exception e) {
            log.debug("Error tracking cache access: {}", e.getMessage());
        }
    }

    /**
     * Check if key exists in Redis with optimized error handling
     */
    private boolean checkRedisKey(String cacheName, Object key) {
        try {
            var redisKey = cacheName + "::" + key;
            return redisTemplate.hasKey(redisKey);
        } catch (Exception e) {
            log.debug("Error checking Redis for key existence: {}", e.getMessage());
            return false;
        }
    }
}
