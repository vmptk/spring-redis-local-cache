package com.example.demo;

import com.example.demo.infra.cache.RedisNearCacheManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.UnifiedJedis;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    @Value("${spring.cache.redis.local-max-size:100}")
    private int localMaxSize;

    @Bean
    @ServiceConnection(name = "redis")
    GenericContainer<?> redisContainer() {
        return new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379)
                .withCommand("redis-server", "--save", "", "--appendonly", "no");
    }

    @Bean
    @Primary
    UnifiedJedis testUnifiedJedis(GenericContainer<?> redisContainer) {
        String host = redisContainer.getHost();
        int port = redisContainer.getMappedPort(6379);
        
        HostAndPort endpoint = new HostAndPort(host, port);
        DefaultJedisClientConfig config = DefaultJedisClientConfig.builder()
                .protocol(RedisProtocol.RESP3)
                .connectionTimeoutMillis(2000)
                .build();
        redis.clients.jedis.csc.CacheConfig cacheConfig = redis.clients.jedis.csc.CacheConfig.builder()
                .maxSize(localMaxSize)
                .build();
        return new UnifiedJedis(endpoint, config, cacheConfig);
    }

    @Bean
    @Primary
    CacheManager testCacheManager(UnifiedJedis testUnifiedJedis, ObjectMapper objectMapper) {
        return new RedisNearCacheManager(testUnifiedJedis, objectMapper);
    }
}