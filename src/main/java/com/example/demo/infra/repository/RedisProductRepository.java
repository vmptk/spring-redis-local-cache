package com.example.demo.infra.repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import com.example.demo.domain.model.Product;
import com.example.demo.domain.model.ProductId;
import com.example.demo.domain.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class RedisProductRepository implements ProductRepository {

    private static final String KEY_PREFIX = "product:";
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Product save(Product product) {
        String key = KEY_PREFIX + product.getId().value();
        redisTemplate.opsForValue().set(key, product);
        return product;
    }

    @Override
    public Optional<Product> findById(ProductId productId) {
        var key = KEY_PREFIX + productId.value();
        var value = redisTemplate.opsForValue().get(key);
        return Optional.ofNullable((Product) value);
    }

    @Override
    public List<Product> findByCategory(String categoryId) {
        return findAll().stream()
                .filter(product -> product.getCategory().id().equals(categoryId))
                .toList();
    }

    @Override
    public List<Product> findAll() {
        var keys = redisTemplate.keys(KEY_PREFIX + "*");

        return keys.stream()
                .map(key -> (Product) redisTemplate.opsForValue().get(key))
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public void deleteById(ProductId productId) {
        var key = KEY_PREFIX + productId.value();
        redisTemplate.delete(key);
    }

    @Override
    public boolean existsById(ProductId productId) {
        var key = KEY_PREFIX + productId.value();
        return redisTemplate.hasKey(key);
    }

    @Override
    public List<Product> findActiveProducts() {
        return findAll().stream().filter(Product::isActive).toList();
    }
}
