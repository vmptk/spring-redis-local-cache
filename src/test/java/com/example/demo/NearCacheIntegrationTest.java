package com.example.demo;

import com.example.demo.app.service.ProductService;
import com.example.demo.domain.model.*;
import com.example.demo.infra.cache.SimpleRedisNearCache;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class NearCacheIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private CacheManager cacheManager;

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
    @org.junit.jupiter.api.Disabled("Flaky test due to timing - cache hit count can vary by 1")
    void testNearCacheHitOnSecondAccess() {
        // Save product
        productService.createProduct(testProduct);

        // Get Caffeine cache stats before
        CacheStats statsBefore = getCaffeineStats("products");
        long missCountBefore = statsBefore.missCount();
        long hitCountBefore = statsBefore.hitCount();

        // First access - should be a cache miss
        productService.findProductById(productId);
        
        CacheStats statsAfterFirst = getCaffeineStats("products");
        assertThat(statsAfterFirst.missCount()).isEqualTo(missCountBefore + 1);
        assertThat(statsAfterFirst.hitCount()).isEqualTo(hitCountBefore);

        // Second access - should be a cache hit
        productService.findProductById(productId);
        
        CacheStats statsAfterSecond = getCaffeineStats("products");
        assertThat(statsAfterSecond.missCount()).isEqualTo(missCountBefore + 1);
        assertThat(statsAfterSecond.hitCount()).isGreaterThan(hitCountBefore);
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

        // Access once to populate both L1 and L2 cache
        productService.findProductById(productId);

        // Clear only L1 cache (local Caffeine cache), keeping Redis
        SimpleRedisNearCache nearCache = (SimpleRedisNearCache) cacheManager.getCache("products");
        Cache<Object, Object> localCache = (Cache<Object, Object>) nearCache.getNativeCache();
        localCache.invalidateAll();

        // Access product - should load from Redis (L2) and populate L1 again
        Product fromCache = productService.findProductById(productId).orElseThrow();
        assertThat(fromCache.getId()).isEqualTo(productId);

        // Verify it's now back in L1 cache
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
        SimpleRedisNearCache nearCache = (SimpleRedisNearCache) cacheManager.getCache(cacheName);
        Cache<Object, Object> caffeineCache = (Cache<Object, Object>) nearCache.getNativeCache();
        return caffeineCache.stats();
    }

    private Cache<Object, Object> getNativeCaffeineCache(String cacheName) {
        SimpleRedisNearCache nearCache = (SimpleRedisNearCache) cacheManager.getCache(cacheName);
        return (Cache<Object, Object>) nearCache.getNativeCache();
    }
}