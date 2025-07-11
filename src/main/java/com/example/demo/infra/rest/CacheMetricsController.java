package com.example.demo.infra.rest;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
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
        
        for (String cacheName : cacheManager.getCacheNames()) {
            org.springframework.cache.Cache springCache = cacheManager.getCache(cacheName);
            
            if (springCache instanceof CaffeineCache caffeineCache) {
                var nativeCache = caffeineCache.getNativeCache();
                var stats = nativeCache.stats();
                
                var cacheMetrics = new HashMap<String, Object>();
                cacheMetrics.put("hitCount", stats.hitCount());
                cacheMetrics.put("missCount", stats.missCount());
                cacheMetrics.put("loadSuccessCount", stats.loadSuccessCount());
                cacheMetrics.put("loadFailureCount", stats.loadFailureCount());
                cacheMetrics.put("totalLoadTime", stats.totalLoadTime());
                cacheMetrics.put("evictionCount", stats.evictionCount());
                cacheMetrics.put("evictionWeight", stats.evictionWeight());
                cacheMetrics.put("hitRate", stats.hitRate());
                cacheMetrics.put("missRate", stats.missRate());
                cacheMetrics.put("estimatedSize", nativeCache.estimatedSize());
                
                metrics.put(cacheName, cacheMetrics);
            }
        }
        
        return metrics;
    }
}