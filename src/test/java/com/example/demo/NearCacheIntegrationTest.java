package com.example.demo;

import com.example.demo.app.service.ProductService;
import com.example.demo.domain.model.*;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.support.CompositeCacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class NearCacheIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private CacheManager cacheManager;
    
    @Autowired
    private CaffeineCacheManager caffeineCacheManager;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private Product testProduct;
    private ProductId productId;

    @BeforeEach
    void setUp() {
        // Clear caches
        cacheManager.getCacheNames().forEach(name -> 
            cacheManager.getCache(name).clear()
        );

        // Clear Redis
        redisTemplate.getConnectionFactory().getConnection().flushAll();

        // Create test product
        ProductDetails details = ProductDetails.create(
                "Test Product",
                "Test Description",
                "Test Brand",
                "TEST-SKU-001"
        );
        Price price = Price.of(99.99, "USD");
        Category category = Category.create("electronics", "Electronics", "Electronic products");
        
        testProduct = Product.create(details, price, category);
        productId = testProduct.getId();
    }

    @Test
    void testNearCacheHitOnSecondAccess() {
        // Get Caffeine cache stats before any operations
        CacheStats statsBefore = getCaffeineStats("products");
        long missCountBefore = statsBefore.missCount();
        long hitCountBefore = statsBefore.hitCount();

        // Save product (may or may not cache)
        productService.createProduct(testProduct);

        // Clear cache to ensure controlled test conditions
        cacheManager.getCache("products").clear();
        
        // Get stats after clearing cache
        CacheStats statsAfterClear = getCaffeineStats("products");
        long missCountAfterClear = statsAfterClear.missCount();
        long hitCountAfterClear = statsAfterClear.hitCount();

        // First access - should be a cache miss (loading from Redis or DB)
        productService.findProductById(productId);
        
        CacheStats statsAfterFirst = getCaffeineStats("products");
        assertThat(statsAfterFirst.missCount()).isGreaterThan(missCountAfterClear);

        // Second access - should be a cache hit
        productService.findProductById(productId);
        
        CacheStats statsAfterSecond = getCaffeineStats("products");
        assertThat(statsAfterSecond.hitCount()).isGreaterThan(hitCountAfterClear);
        assertThat(statsAfterSecond.missCount()).isEqualTo(statsAfterFirst.missCount());
    }

    @Test
    void testCacheEvictionOnUpdate() {
        // Save product
        productService.createProduct(testProduct);

        // First access to populate cache
        productService.findProductById(productId);

        // Update price - should evict from cache
        Price newPrice = Price.of(79.99, "USD");
        productService.updateProductPrice(productId, newPrice);

        // Get updated product
        Product updatedProduct = productService.findProductById(productId).orElseThrow();
        
        // Verify price was updated
        assertThat(updatedProduct.getPrice()).isEqualTo(newPrice);
    }

    @Test
    void testCacheEvictionOnDelete() {
        // Save product
        productService.createProduct(testProduct);

        // Access to populate cache
        assertThat(productService.findProductById(productId)).isPresent();

        // Delete product - should evict from cache
        productService.deleteProduct(productId);

        // Verify product is not found
        assertThat(productService.findProductById(productId)).isEmpty();
    }

    @Test
    void testCategoryQueryCaching() {
        // Create multiple products in same category
        Product product1 = productService.createProduct(testProduct);
        
        ProductDetails details2 = ProductDetails.create("Product 2", "Desc 2", "Brand 2", "SKU-002");
        Product product2 = Product.create(details2, Price.of(149.99, "USD"), product1.getCategory());
        productService.createProduct(product2);

        // Get cache stats before
        CacheStats statsBefore = getCaffeineStats("products");
        long missCountBefore = statsBefore.missCount();

        // First query - cache miss
        productService.findProductsByCategory("electronics");
        
        CacheStats statsAfterFirst = getCaffeineStats("products");
        assertThat(statsAfterFirst.missCount()).isEqualTo(missCountBefore + 1);

        // Second query - cache hit
        productService.findProductsByCategory("electronics");
        
        CacheStats statsAfterSecond = getCaffeineStats("products");
        assertThat(statsAfterSecond.hitCount()).isEqualTo(statsBefore.hitCount() + 1);
    }

    @Test
    void testRedisAsL2Cache() {
        // Save product
        productService.createProduct(testProduct);

        // Clear L1 cache (Caffeine)
        cacheManager.getCache("products").clear();

        // Access product - should load from Redis (L2)
        Product fromCache = productService.findProductById(productId).orElseThrow();
        assertThat(fromCache.getId()).isEqualTo(productId);

        // Verify it's now in L1 cache
        CacheStats stats = getCaffeineStats("products");
        assertThat(stats.missCount()).isGreaterThan(0);
    }

    @Test
    void testCacheEvictAll() {
        // Create multiple products
        productService.createProduct(testProduct);
        
        ProductDetails details2 = ProductDetails.create("Product 2", "Desc 2", "Brand 2", "SKU-002");
        Product product2 = Product.create(details2, Price.of(149.99, "USD"), testProduct.getCategory());
        productService.createProduct(product2);

        // Access to populate cache
        productService.findProductById(productId);
        productService.findProductById(product2.getId());

        // Verify cache has entries
        Cache<Object, Object> cache = getNativeCaffeineCache("products");
        assertThat(cache.estimatedSize()).isGreaterThan(0);

        // Evict all
        productService.evictAllProductsCache();

        // Verify cache is empty
        assertThat(cache.estimatedSize()).isEqualTo(0);
    }

    private CacheStats getCaffeineStats(String cacheName) {
        CaffeineCache caffeineCache = (CaffeineCache) caffeineCacheManager.getCache(cacheName);
        return caffeineCache.getNativeCache().stats();
    }

    private Cache<Object, Object> getNativeCaffeineCache(String cacheName) {
        CaffeineCache caffeineCache = (CaffeineCache) caffeineCacheManager.getCache(cacheName);
        return caffeineCache.getNativeCache();
    }
}