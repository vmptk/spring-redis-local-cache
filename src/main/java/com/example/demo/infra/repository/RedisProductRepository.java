package com.example.demo.infra.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
        String key = KEY_PREFIX + productId.value();
        Object value = redisTemplate.opsForValue().get(key);
        return Optional.ofNullable((Product) value);
    }

    @Override
    public List<Product> findByCategory(String categoryId) {
        return findAll().stream()
                .filter(product -> product.getCategory().id().equals(categoryId))
                .collect(Collectors.toList());
    }

    @Override
    public List<Product> findAll() {
        Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
        if (keys == null) {
            return List.of();
        }

        return keys.stream()
                .map(key -> (Product) redisTemplate.opsForValue().get(key))
                .filter(product -> product != null)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(ProductId productId) {
        String key = KEY_PREFIX + productId.value();
        redisTemplate.delete(key);
    }

    @Override
    public boolean existsById(ProductId productId) {
        String key = KEY_PREFIX + productId.value();
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    @Override
    public List<Product> findActiveProducts() {
        return findAll().stream().filter(Product::isActive).collect(Collectors.toList());
    }
}
