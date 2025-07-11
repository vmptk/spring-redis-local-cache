package com.example.demo.infra.config;

import com.example.demo.infra.cache.RedisNearCacheManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class SchedulingConfig {
    
    private final CacheManager cacheManager;
    
    @Scheduled(fixedRate = 60000) // Every minute
    public void logCacheStats() {
        if (cacheManager instanceof RedisNearCacheManager redisNearCacheManager) {
            try {
                var jedisCache = redisNearCacheManager.getJedisCache();
                log.debug("Cache stats - Size: {}, Stats: {}", 
                    jedisCache.getSize(), jedisCache.getStats());
            } catch (Exception e) {
                log.debug("Could not get cache stats", e);
            }
        }
    }
}