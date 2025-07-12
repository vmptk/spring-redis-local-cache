package com.example.demo.app.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheStatisticsService {

    private final CacheManager cacheManager;
    private final CaffeineCacheManager caffeineCacheManager;
    private final RedisTemplate<String, Object> redisTemplate;
    
    // Track cache access patterns
    private final Map<String, CacheAccessStats> cacheAccessStats = new ConcurrentHashMap<>();
    private final String instanceId = UUID.randomUUID().toString().substring(0, 8);
    
    @Data
    public static class CacheAccessStats {
        private final AtomicLong totalAccesses = new AtomicLong(0);
        private final AtomicLong l1Hits = new AtomicLong(0);
        private final AtomicLong l1Misses = new AtomicLong(0);
        private final AtomicLong l2Hits = new AtomicLong(0);
        private final AtomicLong l2Misses = new AtomicLong(0);
        private LocalDateTime lastAccess;
        
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
            long total = totalAccesses.get();
            return total > 0 ? (double) l1Hits.get() / total * 100 : 0;
        }
        
        public double getL2HitRate() {
            long l1MissCount = l1Misses.get();
            return l1MissCount > 0 ? (double) l2Hits.get() / l1MissCount * 100 : 0;
        }
        
        public double getOverallHitRate() {
            long total = totalAccesses.get();
            long totalHits = l1Hits.get() + l2Hits.get();
            return total > 0 ? (double) totalHits / total * 100 : 0;
        }
    }
    
    @Data
    public static class DetailedCacheStatistics {
        private String instanceId;
        private String timestamp;
        private Map<String, CacheMetrics> cacheMetrics = new HashMap<>();
        private Map<String, CacheAccessMetrics> accessMetrics = new HashMap<>();
        private RedisMetrics redisMetrics;
    }
    
    @Data
    public static class CacheMetrics {
        private long size;
        private long hitCount;
        private long missCount;
        private double hitRate;
        private long evictionCount;
        private double averageLoadPenalty;
        private long requestCount;
    }
    
    @Data
    public static class CacheAccessMetrics {
        private long totalAccesses;
        private long l1Hits;
        private long l1Misses;
        private long l2Hits;
        private long l2Misses;
        private double l1HitRate;
        private double l2HitRate;
        private double overallHitRate;
        private String lastAccess;
    }
    
    @Data
    public static class RedisMetrics {
        private long keyCount;
        private long connectedClients;
        private long commandsProcessed;
        private double hitRate;
    }
    
    public void recordCacheAccess(String cacheName, CacheAccessType accessType) {
        CacheAccessStats stats = cacheAccessStats.computeIfAbsent(cacheName, k -> new CacheAccessStats());
        
        switch (accessType) {
            case L1_HIT:
                stats.recordL1Hit();
                log.debug("[Instance: {}] Cache '{}' - L1 HIT", instanceId, cacheName);
                break;
            case L1_MISS_L2_HIT:
                stats.recordL1MissL2Hit();
                log.debug("[Instance: {}] Cache '{}' - L1 MISS, L2 HIT", instanceId, cacheName);
                break;
            case L1_MISS_L2_MISS:
                stats.recordL1MissL2Miss();
                log.debug("[Instance: {}] Cache '{}' - L1 MISS, L2 MISS", instanceId, cacheName);
                break;
        }
    }
    
    public DetailedCacheStatistics getDetailedStatistics() {
        DetailedCacheStatistics statistics = new DetailedCacheStatistics();
        statistics.setInstanceId(instanceId);
        statistics.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        // Collect Caffeine cache metrics
        Map<String, CacheMetrics> cacheMetricsMap = new HashMap<>();
        cacheManager.getCacheNames().forEach(cacheName -> {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null && cache.getNativeCache() instanceof Cache<?, ?> caffeineCache) {
                CacheStats stats = caffeineCache.stats();
                
                CacheMetrics metrics = new CacheMetrics();
                metrics.setSize(caffeineCache.estimatedSize());
                metrics.setHitCount(stats.hitCount());
                metrics.setMissCount(stats.missCount());
                metrics.setHitRate(stats.hitRate() * 100);
                metrics.setEvictionCount(stats.evictionCount());
                metrics.setAverageLoadPenalty(stats.averageLoadPenalty() / 1_000_000.0); // Convert to milliseconds
                metrics.setRequestCount(stats.requestCount());
                
                cacheMetricsMap.put(cacheName, metrics);
            }
        });
        statistics.setCacheMetrics(cacheMetricsMap);
        
        // Collect access pattern metrics
        Map<String, CacheAccessMetrics> accessMetricsMap = new HashMap<>();
        cacheAccessStats.forEach((cacheName, stats) -> {
            CacheAccessMetrics metrics = new CacheAccessMetrics();
            metrics.setTotalAccesses(stats.getTotalAccesses().get());
            metrics.setL1Hits(stats.getL1Hits().get());
            metrics.setL1Misses(stats.getL1Misses().get());
            metrics.setL2Hits(stats.getL2Hits().get());
            metrics.setL2Misses(stats.getL2Misses().get());
            metrics.setL1HitRate(stats.getL1HitRate());
            metrics.setL2HitRate(stats.getL2HitRate());
            metrics.setOverallHitRate(stats.getOverallHitRate());
            metrics.setLastAccess(stats.getLastAccess() != null ? 
                stats.getLastAccess().format(DateTimeFormatter.ISO_LOCAL_TIME) : "Never");
            
            accessMetricsMap.put(cacheName, metrics);
        });
        statistics.setAccessMetrics(accessMetricsMap);
        
        // Collect Redis metrics
        try {
            RedisMetrics redisMetrics = new RedisMetrics();
            Set<String> keys = redisTemplate.keys("*");
            redisMetrics.setKeyCount(keys != null ? keys.size() : 0);
            
            // Get Redis INFO stats
            Properties info = redisTemplate.getConnectionFactory().getConnection().commands().info("stats");
            if (info != null) {
                redisMetrics.setConnectedClients(Long.parseLong(info.getProperty("connected_clients", "0")));
                redisMetrics.setCommandsProcessed(Long.parseLong(info.getProperty("total_commands_processed", "0")));
                
                long keyspaceHits = Long.parseLong(info.getProperty("keyspace_hits", "0"));
                long keyspaceMisses = Long.parseLong(info.getProperty("keyspace_misses", "0"));
                long total = keyspaceHits + keyspaceMisses;
                redisMetrics.setHitRate(total > 0 ? (double) keyspaceHits / total * 100 : 0);
            }
            
            statistics.setRedisMetrics(redisMetrics);
        } catch (Exception e) {
            log.error("Error collecting Redis metrics", e);
        }
        
        return statistics;
    }
    
    public void logDetailedStatistics() {
        log.info("[Instance: {}] 📊 === DETAILED CACHE STATISTICS ===", instanceId);
        
        // Log Caffeine cache stats
        cacheManager.getCacheNames().forEach(cacheName -> {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null && cache.getNativeCache() instanceof Cache<?, ?> caffeineCache) {
                CacheStats stats = caffeineCache.stats();
                log.info("[Instance: {}] 📈 L1 Cache '{}': Size={}, Hits={}, Misses={}, Hit Rate={:.2f}%, Evictions={}", 
                    instanceId, 
                    cacheName,
                    caffeineCache.estimatedSize(),
                    stats.hitCount(),
                    stats.missCount(),
                    stats.hitRate() * 100,
                    stats.evictionCount()
                );
            }
        });
        
        // Log access pattern stats
        cacheAccessStats.forEach((cacheName, stats) -> {
            log.info("[Instance: {}] 🎯 Cache '{}' Access Patterns: Total={}, L1 Hit Rate={:.2f}%, L2 Hit Rate={:.2f}%, Overall Hit Rate={:.2f}%", 
                instanceId,
                cacheName,
                stats.getTotalAccesses().get(),
                stats.getL1HitRate(),
                stats.getL2HitRate(),
                stats.getOverallHitRate()
            );
            log.info("[Instance: {}]    └─ L1: Hits={}, Misses={} | L2: Hits={}, Misses={}", 
                instanceId,
                stats.getL1Hits().get(),
                stats.getL1Misses().get(),
                stats.getL2Hits().get(),
                stats.getL2Misses().get()
            );
        });
        
        // Log Redis stats
        try {
            Set<String> keys = redisTemplate.keys("*");
            Properties info = redisTemplate.getConnectionFactory().getConnection().commands().info("stats");
            if (info != null) {
                long keyspaceHits = Long.parseLong(info.getProperty("keyspace_hits", "0"));
                long keyspaceMisses = Long.parseLong(info.getProperty("keyspace_misses", "0"));
                long total = keyspaceHits + keyspaceMisses;
                double hitRate = total > 0 ? (double) keyspaceHits / total * 100 : 0;
                
                log.info("[Instance: {}] 🔴 Redis Stats: Keys={}, Hits={}, Misses={}, Hit Rate={:.2f}%, Commands Processed={}", 
                    instanceId,
                    keys != null ? keys.size() : 0,
                    keyspaceHits,
                    keyspaceMisses,
                    hitRate,
                    info.getProperty("total_commands_processed", "0")
                );
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