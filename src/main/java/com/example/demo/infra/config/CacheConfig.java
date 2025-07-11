package com.example.demo.infra.config;

import com.example.demo.infra.cache.RedisNearCacheManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.UnifiedJedis;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${spring.cache.redis.local-max-size:1000}")
    private int localMaxSize;
    
    @Value("${spring.redis.host:localhost}")
    private String redisHost;
    
    @Value("${spring.redis.port:6379}")
    private int redisPort;

    @Bean
    public UnifiedJedis unifiedJedis() {
        HostAndPort endpoint = new HostAndPort(redisHost, redisPort);
        DefaultJedisClientConfig config = DefaultJedisClientConfig.builder()
                .protocol(RedisProtocol.RESP3)
                .build();
        redis.clients.jedis.csc.CacheConfig cacheConfig = redis.clients.jedis.csc.CacheConfig.builder()
                .maxSize(localMaxSize)
                .build();
        return new UnifiedJedis(endpoint, config, cacheConfig);
    }

    @Bean("cacheObjectMapper")
    public ObjectMapper cacheObjectMapper() {
        return new ObjectMapper();
    }

    @Bean
    @Primary
    public CacheManager cacheManager(UnifiedJedis unifiedJedis, @Qualifier("cacheObjectMapper") ObjectMapper cacheObjectMapper) {
        return new RedisNearCacheManager(unifiedJedis, cacheObjectMapper);
    }
}