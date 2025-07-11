package com.example.demo.infra.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import redis.clients.jedis.UnifiedJedis;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
public class RedisNearCacheManager implements CacheManager {
    
    private final UnifiedJedis unifiedJedis;
    private final ObjectMapper objectMapper;
    private final ConcurrentMap<String, Cache> cacheMap = new ConcurrentHashMap<>();
    
    public RedisNearCacheManager(UnifiedJedis unifiedJedis, ObjectMapper objectMapper) {
        this.unifiedJedis = unifiedJedis;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public Cache getCache(String name) {
        return cacheMap.computeIfAbsent(name, cacheName -> {
            log.info("Creating Redis near cache: {}", cacheName);
            return new RedisNearCache(cacheName, unifiedJedis, objectMapper);
        });
    }
    
    @Override
    public Collection<String> getCacheNames() {
        return cacheMap.keySet();
    }
    
    public redis.clients.jedis.csc.Cache getJedisCache() {
        return unifiedJedis.getCache();
    }
}