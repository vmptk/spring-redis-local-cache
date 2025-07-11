package com.example.demo.infra.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RedissonClient;
import org.redisson.spring.cache.RedissonSpringCacheManager;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    @Primary
    public CacheManager cacheManager(RedissonClient redissonClient) {
        Map<String, org.redisson.spring.cache.CacheConfig> configs = new HashMap<>();
        
        configs.put("products", new org.redisson.spring.cache.CacheConfig(
                300_000,
                300_000
        ));
        
        configs.put("catalogs", new org.redisson.spring.cache.CacheConfig(
                300_000,
                300_000
        ));
        
        return new RedissonSpringCacheManager(redissonClient, configs);
    }
}