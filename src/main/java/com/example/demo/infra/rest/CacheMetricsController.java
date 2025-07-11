package com.example.demo.infra.rest;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RedissonClient;
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
    private final RedissonClient redissonClient;

    @GetMapping
    public Map<String, Object> getCacheMetrics() {
        var metrics = new HashMap<String, Object>();
        
        for (String cacheName : cacheManager.getCacheNames()) {
            try {
                var map = redissonClient.getMap(cacheName);
                
                var cacheMetrics = new HashMap<String, Object>();
                cacheMetrics.put("size", map.size());
                cacheMetrics.put("isEmpty", map.isEmpty());
                cacheMetrics.put("isExists", map.isExists());
                
                metrics.put(cacheName, cacheMetrics);
            } catch (Exception e) {
                var errorMetrics = new HashMap<String, Object>();
                errorMetrics.put("error", "Unable to retrieve metrics: " + e.getMessage());
                metrics.put(cacheName, errorMetrics);
            }
        }
        
        return metrics;
    }
}