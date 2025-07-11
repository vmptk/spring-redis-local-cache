package com.example.demo.infra.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@RequiredArgsConstructor
public class SimpleRedisNearCacheManager implements CacheManager {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisMessageListenerContainer listenerContainer;
    private final Duration localCacheTtl;
    private final Duration redisTtl;
    private final int maxLocalCacheSize;
    
    private final ConcurrentMap<String, Cache> caches = new ConcurrentHashMap<>();
    
    @Override
    public Cache getCache(String name) {
        return caches.computeIfAbsent(name, this::createCache);
    }
    
    @Override
    public Collection<String> getCacheNames() {
        return caches.keySet();
    }
    
    private Cache createCache(String name) {
        log.info("Creating Redis near cache: {}", name);
        return new SimpleRedisNearCache(
            name,
            redisTemplate,
            localCacheTtl,
            redisTtl,
            maxLocalCacheSize,
            listenerContainer
        );
    }
}