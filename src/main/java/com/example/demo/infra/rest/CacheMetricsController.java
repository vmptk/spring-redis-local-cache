package com.example.demo.infra.rest;

import com.example.demo.infra.cache.RedisNearCacheManager;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/cache/metrics")
@RequiredArgsConstructor
public class CacheMetricsController {
    
    private final CacheManager cacheManager;

    @GetMapping
    public Map<String, Object> getCacheMetrics() {
        var metrics = new HashMap<String, Object>();
        
        if (cacheManager instanceof RedisNearCacheManager redisNearCacheManager) {
            try {
                var jedisCache = redisNearCacheManager.getJedisCache();
                
                var cacheMetrics = new HashMap<String, Object>();
                cacheMetrics.put("cacheType", "UnifiedJedis with Client-Side Caching");
                cacheMetrics.put("protocol", "RESP3");
                cacheMetrics.put("cacheSize", jedisCache.getSize());
                cacheMetrics.put("cacheStats", jedisCache.getStats().toString());
                cacheMetrics.put("status", "active");
                
                metrics.put("jedisCache", cacheMetrics);
                
                // Add Spring cache names
                var springCaches = new HashMap<String, String>();
                for (String cacheName : cacheManager.getCacheNames()) {
                    springCaches.put(cacheName, "Redis Near Cache");
                }
                metrics.put("springCaches", springCaches);
                
            } catch (Exception e) {
                metrics.put("error", "Could not retrieve cache metrics: " + e.getMessage());
            }
        } else {
            metrics.put("error", "Cache manager is not RedisNearCacheManager");
        }
        
        return metrics;
    }
}