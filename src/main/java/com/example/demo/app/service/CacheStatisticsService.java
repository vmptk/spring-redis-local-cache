package com.example.demo.app.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheStatisticsService {

    private final CacheManager cacheManager;
    private final RedisTemplate<String, Object> redisTemplate;

    // Track cache access patterns
    private final Map<String, CacheAccessStats> cacheAccessStats = new ConcurrentHashMap<>();
    private final String instanceId = UUID.randomUUID().toString().substring(0, 8);

    public static final class CacheAccessStats {
        private final AtomicLong totalAccesses = new AtomicLong(0);
        private final AtomicLong l1Hits = new AtomicLong(0);
        private final AtomicLong l1Misses = new AtomicLong(0);
        private final AtomicLong l2Hits = new AtomicLong(0);
        private final AtomicLong l2Misses = new AtomicLong(0);
        private volatile LocalDateTime lastAccess;

        public void recordL1Hit() {
            totalAccesses.incrementAndGet();
            l1Hits.incrementAndGet();
            lastAccess = LocalDateTime.now();
        }

        public void recordL1MissL2Hit() {
            totalAccesses.incrementAndGet();
            l1Misses.incrementAndGet();
            l2Hits.incrementAndGet();
            lastAccess = LocalDateTime.now();
        }

        public void recordL1MissL2Miss() {
            totalAccesses.incrementAndGet();
            l1Misses.incrementAndGet();
            l2Misses.incrementAndGet();
            lastAccess = LocalDateTime.now();
        }

        public double getL1HitRate() {
            var total = totalAccesses.get();
            return total > 0 ? (double) l1Hits.get() / total * 100 : 0;
        }

        public double getL2HitRate() {
            var l1MissCount = l1Misses.get();
            return l1MissCount > 0 ? (double) l2Hits.get() / l1MissCount * 100 : 0;
        }

        public double getOverallHitRate() {
            var total = totalAccesses.get();
            var totalHits = l1Hits.get() + l2Hits.get();
            return total > 0 ? (double) totalHits / total * 100 : 0;
        }

        // Getters for accessing atomic values
        public AtomicLong getTotalAccesses() {
            return totalAccesses;
        }

        public AtomicLong getL1Hits() {
            return l1Hits;
        }

        public AtomicLong getL1Misses() {
            return l1Misses;
        }

        public AtomicLong getL2Hits() {
            return l2Hits;
        }

        public AtomicLong getL2Misses() {
            return l2Misses;
        }

        public LocalDateTime getLastAccess() {
            return lastAccess;
        }
    }

    public record DetailedCacheStatistics(
            String instanceId,
            String timestamp,
            Map<String, CacheMetrics> cacheMetrics,
            Map<String, CacheAccessMetrics> accessMetrics,
            RedisMetrics redisMetrics) {}

    public record CacheMetrics(
            long size,
            long hitCount,
            long missCount,
            double hitRate,
            long evictionCount,
            double averageLoadPenalty,
            long requestCount) {}

    public record CacheAccessMetrics(
            long totalAccesses,
            long l1Hits,
            long l1Misses,
            long l2Hits,
            long l2Misses,
            double l1HitRate,
            double l2HitRate,
            double overallHitRate,
            String lastAccess) {}

    public record RedisMetrics(long keyCount, long connectedClients, long commandsProcessed, double hitRate) {}

    public void recordCacheAccess(String cacheName, CacheAccessType accessType) {
        var stats = cacheAccessStats.computeIfAbsent(cacheName, k -> new CacheAccessStats());

        switch (accessType) {
            case L1_HIT -> {
                stats.recordL1Hit();
                log.debug("[Instance: {}] Cache '{}' - L1 HIT", instanceId, cacheName);
            }
            case L1_MISS_L2_HIT -> {
                stats.recordL1MissL2Hit();
                log.debug("[Instance: {}] Cache '{}' - L1 MISS, L2 HIT", instanceId, cacheName);
            }
            case L1_MISS_L2_MISS -> {
                stats.recordL1MissL2Miss();
                log.debug("[Instance: {}] Cache '{}' - L1 MISS, L2 MISS", instanceId, cacheName);
            }
        }
    }

    public DetailedCacheStatistics getDetailedStatistics() {
        var timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        // Collect Caffeine cache metrics using modern patterns
        var cacheMetricsMap = new HashMap<String, CacheMetrics>();
        cacheManager.getCacheNames().forEach(cacheName -> {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null && cache.getNativeCache() instanceof Cache<?, ?> caffeineCache) {
                var stats = caffeineCache.stats();
                var metrics = new CacheMetrics(
                        caffeineCache.estimatedSize(),
                        stats.hitCount(),
                        stats.missCount(),
                        stats.hitRate() * 100,
                        stats.evictionCount(),
                        stats.averageLoadPenalty() / 1_000_000.0, // Convert to milliseconds
                        stats.requestCount());
                cacheMetricsMap.put(cacheName, metrics);
            }
        });

        // Collect access pattern metrics using modern patterns
        var accessMetricsMap = new HashMap<String, CacheAccessMetrics>();
        cacheAccessStats.forEach((cacheName, stats) -> {
            var metrics = new CacheAccessMetrics(
                    stats.getTotalAccesses().get(),
                    stats.getL1Hits().get(),
                    stats.getL1Misses().get(),
                    stats.getL2Hits().get(),
                    stats.getL2Misses().get(),
                    stats.getL1HitRate(),
                    stats.getL2HitRate(),
                    stats.getOverallHitRate(),
                    stats.getLastAccess() != null
                            ? stats.getLastAccess().format(DateTimeFormatter.ISO_LOCAL_TIME)
                            : "Never");
            accessMetricsMap.put(cacheName, metrics);
        });

        // Collect Redis metrics
        var redisMetrics = collectRedisMetrics();

        return new DetailedCacheStatistics(instanceId, timestamp, cacheMetricsMap, accessMetricsMap, redisMetrics);
    }

    private RedisMetrics collectRedisMetrics() {
        try {
            var keys = redisTemplate.keys("*");
            var keyCount = keys != null ? keys.size() : 0;

            // Get Redis INFO stats
            var info = redisTemplate
                    .getConnectionFactory()
                    .getConnection()
                    .commands()
                    .info("stats");
            if (info != null) {
                var connectedClients = Long.parseLong(info.getProperty("connected_clients", "0"));
                var commandsProcessed = Long.parseLong(info.getProperty("total_commands_processed", "0"));

                var keyspaceHits = Long.parseLong(info.getProperty("keyspace_hits", "0"));
                var keyspaceMisses = Long.parseLong(info.getProperty("keyspace_misses", "0"));
                var total = keyspaceHits + keyspaceMisses;
                var hitRate = total > 0 ? (double) keyspaceHits / total * 100 : 0;

                return new RedisMetrics(keyCount, connectedClients, commandsProcessed, hitRate);
            }
            return new RedisMetrics(keyCount, 0, 0, 0);
        } catch (Exception e) {
            log.error("Error collecting Redis metrics", e);
            return new RedisMetrics(0, 0, 0, 0);
        }
    }

    public void logDetailedStatistics() {
        log.info("[Instance: {}] 📊 === DETAILED CACHE STATISTICS ===", instanceId);

        // Log Caffeine cache stats
        cacheManager.getCacheNames().forEach(cacheName -> {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null && cache.getNativeCache() instanceof Cache<?, ?> caffeineCache) {
                var stats = caffeineCache.stats();
                log.info(
                        "[Instance: {}] 📈 L1 Cache '{}': Size={}, Hits={}, Misses={}, Hit Rate={:.2f}%, Evictions={}",
                        instanceId,
                        cacheName,
                        caffeineCache.estimatedSize(),
                        stats.hitCount(),
                        stats.missCount(),
                        stats.hitRate() * 100,
                        stats.evictionCount());
            }
        });

        // Log access pattern stats
        cacheAccessStats.forEach((cacheName, stats) -> {
            log.info(
                    "[Instance: {}] 🎯 Cache '{}' Access Patterns: Total={}, L1 Hit Rate={:.2f}%, L2 Hit Rate={:.2f}%, Overall Hit Rate={:.2f}%",
                    instanceId,
                    cacheName,
                    stats.getTotalAccesses().get(),
                    stats.getL1HitRate(),
                    stats.getL2HitRate(),
                    stats.getOverallHitRate());
            log.info(
                    "[Instance: {}]    └─ L1: Hits={}, Misses={} | L2: Hits={}, Misses={}",
                    instanceId,
                    stats.getL1Hits().get(),
                    stats.getL1Misses().get(),
                    stats.getL2Hits().get(),
                    stats.getL2Misses().get());
        });

        // Log Redis stats
        try {
            var keys = redisTemplate.keys("*");
            var info = redisTemplate
                    .getConnectionFactory()
                    .getConnection()
                    .commands()
                    .info("stats");
            if (info != null) {
                var keyspaceHits = Long.parseLong(info.getProperty("keyspace_hits", "0"));
                var keyspaceMisses = Long.parseLong(info.getProperty("keyspace_misses", "0"));
                var total = keyspaceHits + keyspaceMisses;
                var hitRate = total > 0 ? (double) keyspaceHits / total * 100 : 0;

                log.info(
                        "[Instance: {}] 🔴 Redis Stats: Keys={}, Hits={}, Misses={}, Hit Rate={:.2f}%, Commands Processed={}",
                        instanceId,
                        keys != null ? keys.size() : 0,
                        keyspaceHits,
                        keyspaceMisses,
                        hitRate,
                        info.getProperty("total_commands_processed", "0"));
            }
        } catch (Exception e) {
            log.error("Error logging Redis statistics", e);
        }

        log.info("[Instance: {}] =========================================", instanceId);
    }

    public enum CacheAccessType {
        L1_HIT,
        L1_MISS_L2_HIT,
        L1_MISS_L2_MISS
    }
}
