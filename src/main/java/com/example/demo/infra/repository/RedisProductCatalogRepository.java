package com.example.demo.infra.repository;

import com.example.demo.domain.model.ProductCatalog;
import com.example.demo.domain.repository.ProductCatalogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
        Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
        if (keys == null) {
            return List.of();
        }
        
        return keys.stream()
                .map(key -> (ProductCatalog) redisTemplate.opsForValue().get(key))
                .filter(catalog -> catalog != null)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(String catalogId) {
        String key = KEY_PREFIX + catalogId;
        redisTemplate.delete(key);
    }

    @Override
    public boolean existsById(String catalogId) {
        String key = KEY_PREFIX + catalogId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}