package com.example.demo.infra.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@Slf4j
public class SimpleRedisNearCache extends AbstractValueAdaptingCache {
    
    private final String name;
    private final Cache<Object, Object> localCache;
    private final RedisTemplate<String, Object> redisTemplate;
    private final Duration redisTtl;
    private final String invalidationTopic;
    
    public SimpleRedisNearCache(String name,
                               RedisTemplate<String, Object> redisTemplate,
                               Duration localTtl,
                               Duration redisTtl,
                               int maxLocalSize,
                               RedisMessageListenerContainer listenerContainer) {
        super(true);
        this.name = name;
        this.redisTemplate = redisTemplate;
        this.redisTtl = redisTtl;
        this.invalidationTopic = "cache:invalidation:" + name;
        
        this.localCache = Caffeine.newBuilder()
                .expireAfterWrite(localTtl.toMillis(), TimeUnit.MILLISECONDS)
                .maximumSize(maxLocalSize)
                .recordStats()
                .build();
        
        setupInvalidationListener(listenerContainer);
    }
    
    private void setupInvalidationListener(RedisMessageListenerContainer listenerContainer) {
        MessageListener listener = new MessageListener() {
            @Override
            public void onMessage(Message message, byte[] pattern) {
                String messageBody = new String(message.getBody());
                log.debug("Received invalidation message: {}", messageBody);
                if ("CLEAR_ALL".equals(messageBody)) {
                    localCache.invalidateAll();
                    log.debug("Cleared all local cache entries for: {}", name);
                } else if (messageBody.startsWith("INVALIDATE:")) {
                    String key = messageBody.substring("INVALIDATE:".length());
                    localCache.invalidate(key);
                    log.debug("Invalidated local cache entry for key: {} in cache: {}", key, name);
                }
            }
        };
        
        listenerContainer.addMessageListener(listener, 
            new org.springframework.data.redis.listener.PatternTopic(invalidationTopic));
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public Object getNativeCache() {
        return localCache;
    }
    
    @Override
    protected Object lookup(Object key) {
        String keyStr = key.toString();
        
        Object localValue = localCache.getIfPresent(keyStr);
        if (localValue != null) {
            log.debug("Local cache hit for key: {}", keyStr);
            return localValue;
        }
        
        log.debug("Local cache miss, fetching from Redis for key: {}", keyStr);
        Object redisValue = redisTemplate.opsForValue().get(keyStr);
        if (redisValue != null) {
            localCache.put(keyStr, redisValue);
            return redisValue;
        }
        
        return null;
    }
    
    @Override
    public void put(Object key, Object value) {
        String keyStr = key.toString();
        
        if (redisTtl != null) {
            redisTemplate.opsForValue().set(keyStr, value, redisTtl);
        } else {
            redisTemplate.opsForValue().set(keyStr, value);
        }
        
        localCache.put(keyStr, value);
        publishInvalidation(keyStr);
    }
    
    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {
        String keyStr = key.toString();
        
        Object existingValue = get(keyStr);
        if (existingValue != null) {
            return toValueWrapper(existingValue);
        }
        
        Boolean wasSet = redisTemplate.opsForValue().setIfAbsent(keyStr, value, redisTtl);
        if (Boolean.TRUE.equals(wasSet)) {
            localCache.put(keyStr, value);
            publishInvalidation(keyStr);
            return null;
        } else {
            return get(keyStr);
        }
    }
    
    @Override
    public void evict(Object key) {
        String keyStr = key.toString();
        redisTemplate.delete(keyStr);
        localCache.invalidate(keyStr);
        publishInvalidation(keyStr);
    }
    
    @Override
    public boolean evictIfPresent(Object key) {
        String keyStr = key.toString();
        Boolean deleted = redisTemplate.delete(keyStr);
        localCache.invalidate(keyStr);
        publishInvalidation(keyStr);
        return Boolean.TRUE.equals(deleted);
    }
    
    @Override
    public void clear() {
        localCache.invalidateAll();
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
        publishClearAll();
    }
    
    @Override
    public boolean invalidate() {
        try {
            clear();
            return true;
        } catch (Exception e) {
            log.error("Error invalidating cache: {}", name, e);
            return false;
        }
    }
    
    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        String keyStr = key.toString();
        
        Object value = lookup(keyStr);
        if (value != null) {
            @SuppressWarnings("unchecked")
            T result = (T) fromStoreValue(value);
            return result;
        }
        
        try {
            T loadedValue = valueLoader.call();
            if (loadedValue != null) {
                put(keyStr, loadedValue);
            }
            return loadedValue;
        } catch (Exception e) {
            throw new ValueRetrievalException(key, valueLoader, e);
        }
    }
    
    private void publishInvalidation(String key) {
        try {
            redisTemplate.convertAndSend(invalidationTopic, "INVALIDATE:" + key);
        } catch (Exception e) {
            log.warn("Failed to publish invalidation for key: {}", key, e);
        }
    }
    
    private void publishClearAll() {
        try {
            redisTemplate.convertAndSend(invalidationTopic, "CLEAR_ALL");
        } catch (Exception e) {
            log.warn("Failed to publish clear all invalidation", e);
        }
    }
}