package com.example.demo.infra.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import redis.clients.jedis.UnifiedJedis;

import java.util.concurrent.Callable;

@Slf4j
public class RedisNearCache implements Cache {
    
    private final String name;
    private final UnifiedJedis unifiedJedis;
    private final ObjectMapper objectMapper;
    
    public RedisNearCache(String name, UnifiedJedis unifiedJedis, ObjectMapper objectMapper) {
        this.name = name;
        this.unifiedJedis = unifiedJedis;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public Object getNativeCache() {
        return unifiedJedis;
    }
    
    @Override
    public ValueWrapper get(Object key) {
        try {
            String redisKey = getRedisKey(key.toString());
            String json = unifiedJedis.get(redisKey);
            if (json != null) {
                Object value = objectMapper.readValue(json, Object.class);
                return () -> value;
            }
        } catch (Exception e) {
            log.error("Failed to get from cache for key: {}", key, e);
        }
        return null;
    }
    
    @Override
    public <T> T get(Object key, Class<T> type) {
        try {
            String redisKey = getRedisKey(key.toString());
            String json = unifiedJedis.get(redisKey);
            if (json != null) {
                return objectMapper.readValue(json, type);
            }
        } catch (Exception e) {
            log.error("Failed to get from cache for key: {}", key, e);
        }
        return null;
    }
    
    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        Object value = get(key, Object.class);
        if (value != null) {
            try {
                @SuppressWarnings("unchecked")
                T typedValue = (T) value;
                return typedValue;
            } catch (ClassCastException e) {
                log.warn("Type mismatch for cached value with key: {}", key, e);
            }
        }
        
        try {
            T loadedValue = valueLoader.call();
            if (loadedValue != null) {
                put(key, loadedValue);
            }
            return loadedValue;
        } catch (Exception e) {
            throw new ValueRetrievalException(key, valueLoader, e);
        }
    }
    
    @Override
    public void put(Object key, Object value) {
        try {
            if (value != null) {
                String redisKey = getRedisKey(key.toString());
                String json = objectMapper.writeValueAsString(value);
                unifiedJedis.set(redisKey, json);
            }
        } catch (Exception e) {
            log.error("Failed to put to cache for key: {}", key, e);
        }
    }
    
    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {
        ValueWrapper existing = get(key);
        if (existing == null && value != null) {
            put(key, value);
            return () -> value;
        }
        return existing;
    }
    
    @Override
    public void evict(Object key) {
        try {
            String redisKey = getRedisKey(key.toString());
            unifiedJedis.del(redisKey);
        } catch (Exception e) {
            log.error("Failed to evict from cache for key: {}", key, e);
        }
    }
    
    @Override
    public boolean evictIfPresent(Object key) {
        try {
            String redisKey = getRedisKey(key.toString());
            Long result = unifiedJedis.del(redisKey);
            return result > 0;
        } catch (Exception e) {
            log.error("Failed to evict from cache for key: {}", key, e);
            return false;
        }
    }
    
    @Override
    public void clear() {
        try {
            String pattern = getRedisKey("*");
            unifiedJedis.eval("for _,k in ipairs(redis.call('keys',ARGV[1])) do redis.call('del',k) end", 0, pattern);
        } catch (Exception e) {
            log.error("Failed to clear cache: {}", name, e);
        }
    }
    
    private String getRedisKey(String key) {
        return "cache:" + name + ":" + key;
    }
}