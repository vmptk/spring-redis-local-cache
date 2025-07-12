package com.example.demo.infra.rest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.app.service.CacheStatisticsService;

import lombok.RequiredArgsConstructor;

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
        summary.append("Instance: ").append(stats.instanceId()).append("\n");
        summary.append("Timestamp: ").append(stats.timestamp()).append("\n\n");

        // L1 Cache Summary
        summary.append("L1 Cache (Caffeine):\n");
        stats.cacheMetrics().forEach((cacheName, metrics) -> {
            summary.append("  ")
                    .append(cacheName)
                    .append(": ")
                    .append("Size=")
                    .append(metrics.size())
                    .append(", ")
                    .append("Hit Rate=")
                    .append(String.format("%.2f%%", metrics.hitRate()))
                    .append(", ")
                    .append("Evictions=")
                    .append(metrics.evictionCount())
                    .append("\n");
        });

        // Access Pattern Summary
        summary.append("\nAccess Patterns:\n");
        stats.accessMetrics().forEach((cacheName, metrics) -> {
            summary.append("  ")
                    .append(cacheName)
                    .append(": ")
                    .append("L1 Hit Rate=")
                    .append(String.format("%.2f%%", metrics.l1HitRate()))
                    .append(", ")
                    .append("L2 Hit Rate=")
                    .append(String.format("%.2f%%", metrics.l2HitRate()))
                    .append(", ")
                    .append("Overall=")
                    .append(String.format("%.2f%%", metrics.overallHitRate()))
                    .append("\n");
        });

        // Redis Summary
        if (stats.redisMetrics() != null) {
            var redis = stats.redisMetrics();
            summary.append("\nRedis (L2 Cache):\n");
            summary.append("  Keys=")
                    .append(redis.keyCount())
                    .append(", ")
                    .append("Hit Rate=")
                    .append(String.format("%.2f%%", redis.hitRate()))
                    .append(", ")
                    .append("Commands=")
                    .append(redis.commandsProcessed())
                    .append("\n");
        }

        return summary.toString();
    }
}
