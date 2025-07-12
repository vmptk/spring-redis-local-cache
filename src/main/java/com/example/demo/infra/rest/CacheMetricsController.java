package com.example.demo.infra.rest;

import com.example.demo.app.service.CacheStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
public class CacheMetricsController {
    
    private final CacheStatisticsService statisticsService;

    @GetMapping("/metrics")
    public CacheStatisticsService.DetailedCacheStatistics getCacheMetrics() {
        return statisticsService.getDetailedStatistics();
    }
    
    @GetMapping("/summary")
    public String getCacheSummary() {
        var stats = statisticsService.getDetailedStatistics();
        StringBuilder summary = new StringBuilder();
        
        summary.append("=== CACHE SUMMARY ===\n");
        summary.append("Instance: ").append(stats.getInstanceId()).append("\n");
        summary.append("Timestamp: ").append(stats.getTimestamp()).append("\n\n");
        
        // L1 Cache Summary
        summary.append("L1 Cache (Caffeine):\n");
        stats.getCacheMetrics().forEach((cacheName, metrics) -> {
            summary.append("  ").append(cacheName).append(": ")
                   .append("Size=").append(metrics.getSize()).append(", ")
                   .append("Hit Rate=").append(String.format("%.2f%%", metrics.getHitRate())).append(", ")
                   .append("Evictions=").append(metrics.getEvictionCount()).append("\n");
        });
        
        // Access Pattern Summary
        summary.append("\nAccess Patterns:\n");
        stats.getAccessMetrics().forEach((cacheName, metrics) -> {
            summary.append("  ").append(cacheName).append(": ")
                   .append("L1 Hit Rate=").append(String.format("%.2f%%", metrics.getL1HitRate())).append(", ")
                   .append("L2 Hit Rate=").append(String.format("%.2f%%", metrics.getL2HitRate())).append(", ")
                   .append("Overall=").append(String.format("%.2f%%", metrics.getOverallHitRate())).append("\n");
        });
        
        // Redis Summary
        if (stats.getRedisMetrics() != null) {
            var redis = stats.getRedisMetrics();
            summary.append("\nRedis (L2 Cache):\n");
            summary.append("  Keys=").append(redis.getKeyCount()).append(", ")
                   .append("Hit Rate=").append(String.format("%.2f%%", redis.getHitRate())).append(", ")
                   .append("Commands=").append(redis.getCommandsProcessed()).append("\n");
        }
        
        return summary.toString();
    }
}