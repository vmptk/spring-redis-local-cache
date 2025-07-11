package com.example.demo.infra.config;

import com.example.demo.infra.cache.RedisNearCacheManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.UnifiedJedis;

@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${spring.cache.redis.local-max-size:1000}")
    private int localMaxSize;

    @Bean
    public UnifiedJedis unifiedJedis(RedisProperties redisProperties) {
        HostAndPort endpoint = new HostAndPort(redisProperties.getHost(), redisProperties.getPort());
        DefaultJedisClientConfig config = DefaultJedisClientConfig.builder()
                .protocol(RedisProtocol.RESP3)
                .connectionTimeoutMillis(redisProperties.getTimeout() != null ? 
                    (int) redisProperties.getTimeout().toMillis() : 2000)
                .build();
        redis.clients.jedis.csc.CacheConfig cacheConfig = redis.clients.jedis.csc.CacheConfig.builder()
                .maxSize(localMaxSize)
                .build();
        return new UnifiedJedis(endpoint, config, cacheConfig);
    }

    @Bean
    public CacheManager cacheManager(UnifiedJedis unifiedJedis, ObjectMapper objectMapper) {
        return new RedisNearCacheManager(unifiedJedis, objectMapper);
    }
}