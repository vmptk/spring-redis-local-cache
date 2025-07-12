package com.example.demo.infra.repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import com.example.demo.domain.model.ProductCatalog;
import com.example.demo.domain.repository.ProductCatalogRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class RedisProductCatalogRepository implements ProductCatalogRepository {

    private static final String KEY_PREFIX = "catalog:";
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public ProductCatalog save(ProductCatalog catalog) {
        String key = KEY_PREFIX + catalog.getCatalogId();
        redisTemplate.opsForValue().set(key, catalog);
        return catalog;
    }

    @Override
    public Optional<ProductCatalog> findById(String catalogId) {
        String key = KEY_PREFIX + catalogId;
        Object value = redisTemplate.opsForValue().get(key);
        return Optional.ofNullable((ProductCatalog) value);
    }

    @Override
    public List<ProductCatalog> findAll() {
        var keys = redisTemplate.keys(KEY_PREFIX + "*");

        return keys.stream()
                .map(key -> (ProductCatalog) redisTemplate.opsForValue().get(key))
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public void deleteById(String catalogId) {
        String key = KEY_PREFIX + catalogId;
        redisTemplate.delete(key);
    }

    @Override
    public boolean existsById(String catalogId) {
        String key = KEY_PREFIX + catalogId;
        return redisTemplate.hasKey(key);
    }
}
