package com.example.demo.infra.config;

import com.example.demo.infra.cache.SimpleRedisNearCacheManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${cache.near.local-ttl:PT5M}")
    private Duration localCacheTtl;
    
    @Value("${cache.near.redis-ttl:PT10M}")
    private Duration redisTtl;
    
    @Value("${cache.near.max-local-size:1000}")
    private int maxLocalCacheSize;


    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        return container;
    }

    @Bean
    @Primary  
    public CacheManager nearCacheManager(RedisTemplate<String, Object> redisTemplate,
                                        RedisMessageListenerContainer listenerContainer) {
        return new SimpleRedisNearCacheManager(
            redisTemplate,
            listenerContainer,
            localCacheTtl,
            redisTtl,
            maxLocalCacheSize
        );
    }

    @Bean
    public RedisCacheManager fallbackRedisCacheManager(RedisConnectionFactory connectionFactory,
                                                       ObjectMapper redisObjectMapper) {
        var config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(redisTtl)
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer(redisObjectMapper)))
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }
}