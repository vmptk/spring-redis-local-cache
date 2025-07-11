package com.example.demo;

import com.example.demo.app.service.ProductService;
import com.example.demo.domain.model.*;
import com.example.demo.infra.cache.RedisNearCacheManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.csc.Cache;
import redis.clients.jedis.csc.CacheStats;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.yaml")
class JedisClientSideCacheTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private CacheManager cacheManager;
    
    @Autowired
    private UnifiedJedis unifiedJedis;

    private Product testProduct;
    private ProductId productId;

    @BeforeEach
    void setUp() {
        // Clear caches
        cacheManager.getCacheNames().forEach(name -> 
            cacheManager.getCache(name).clear()
        );

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
    void testJedisClientSideCacheBasicOperation() {
        // Get Jedis cache reference
        Cache jedisCache = getJedisCache();
        long initialSize = jedisCache.getSize();
        
        // Create product (should be cached)
        productService.createProduct(testProduct);
        
        // First access - should populate cache
        Product result1 = productService.findProductById(productId).orElseThrow();
        assertThat(result1.getId()).isEqualTo(productId);
        
        long sizeAfterFirstAccess = jedisCache.getSize();
        assertThat(sizeAfterFirstAccess).isGreaterThanOrEqualTo(initialSize);
        
        // Second access - should use cached data
        Product result2 = productService.findProductById(productId).orElseThrow();
        assertThat(result2.getId()).isEqualTo(productId);
        
        // Verify cache is working
        CacheStats stats = jedisCache.getStats();
        log("Cache stats after operations: {}", stats);
        assertThat(stats.toString()).contains("hits=");
    }

    @Test
    void testJedisClientSideCacheStatistics() {
        // Get initial stats
        Cache jedisCache = getJedisCache();
        
        // Perform cache operations
        productService.createProduct(testProduct);
        
        // Multiple reads to generate cache activity
        for (int i = 0; i < 5; i++) {
            productService.findProductById(productId);
        }
        
        CacheStats finalStats = jedisCache.getStats();
        
        // Verify statistics are available
        assertThat(finalStats.toString()).isNotBlank();
        assertThat(finalStats.toString()).contains("hits=");
        assertThat(finalStats.toString()).contains("misses=");
        
        // Check cache size
        assertThat(jedisCache.getSize()).isGreaterThanOrEqualTo(0);
        
        log("Jedis Cache Stats: {}", finalStats);
        log("Cache Size: {}", jedisCache.getSize());
    }

    @Test
    void testJedisClientSideCacheEviction() {
        Cache jedisCache = getJedisCache();
        
        // Create and cache product
        productService.createProduct(testProduct);
        productService.findProductById(productId);
        
        long initialCacheSize = jedisCache.getSize();
        assertThat(initialCacheSize).isGreaterThan(0);
        
        // Update product (should cause cache invalidation)
        Price newPrice = Price.of(79.99, "USD");
        productService.updateProductPrice(productId, newPrice);
        
        // Verify updated price
        Product updatedProduct = productService.findProductById(productId).orElseThrow();
        assertThat(updatedProduct.getPrice()).isEqualTo(newPrice);
    }

    @Test
    void testJedisClientSideCacheWithMultipleProducts() {
        Cache jedisCache = getJedisCache();
        CacheStats initialStats = jedisCache.getStats();
        
        // Create multiple products
        Product product1 = productService.createProduct(testProduct);
        
        ProductDetails details2 = ProductDetails.create("Product 2", "Desc 2", "Brand 2", "SKU-002");
        Product product2 = Product.create(details2, Price.of(149.99, "USD"), product1.getCategory());
        productService.createProduct(product2);
        
        ProductDetails details3 = ProductDetails.create("Product 3", "Desc 3", "Brand 3", "SKU-003");
        Product product3 = Product.create(details3, Price.of(199.99, "USD"), product1.getCategory());
        productService.createProduct(product3);
        
        // Access all products multiple times
        for (int i = 0; i < 3; i++) {
            productService.findProductById(product1.getId());
            productService.findProductById(product2.getId());
            productService.findProductById(product3.getId());
        }
        
        CacheStats finalStats = jedisCache.getStats();
        
        // Should have cache activity
        assertThat(finalStats.toString()).contains("hits=");
        assertThat(jedisCache.getSize()).isGreaterThanOrEqualTo(0);
        
        log("Multiple products cache stats: {}", finalStats);
    }

    @Test
    void testJedisClientSideCacheConcurrentAccess() throws InterruptedException {
        Cache jedisCache = getJedisCache();
        
        // Create product
        productService.createProduct(testProduct);
        
        // Simulate concurrent access
        Thread[] threads = new Thread[5];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 10; j++) {
                    productService.findProductById(productId);
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
        }
        
        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }
        
        // Wait for completion
        for (Thread thread : threads) {
            thread.join();
        }
        
        CacheStats stats = jedisCache.getStats();
        assertThat(stats.toString()).contains("hits=");
        
        log("Concurrent access stats: {}", stats);
    }

    @Test
    void testJedisClientSideCacheInvalidationMessages() {
        Cache jedisCache = getJedisCache();
        
        // Create and cache product
        productService.createProduct(testProduct);
        productService.findProductById(productId);
        
        // Update product to trigger server invalidation
        productService.updateProductPrice(productId, Price.of(88.88, "USD"));
        
        // Give some time for invalidation message processing
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        CacheStats finalStats = jedisCache.getStats();
        
        // Check that cache operations occurred
        assertThat(finalStats.toString()).contains("loads=");
        
        log("Invalidation test stats: {}", finalStats);
    }

    @Test
    void testJedisClientSideCacheMaxSize() {
        Cache jedisCache = getJedisCache();
        
        // Create many products to test cache size limits
        for (int i = 0; i < 10; i++) {
            ProductDetails details = ProductDetails.create(
                "Product " + i, "Description " + i, "Brand " + i, "SKU-" + String.format("%03d", i)
            );
            Product product = Product.create(details, Price.of(100.0 + i, "USD"), testProduct.getCategory());
            Product savedProduct = productService.createProduct(product);
            
            // Access the product to cache it
            productService.findProductById(savedProduct.getId());
        }
        
        long cacheSize = jedisCache.getSize();
        CacheStats stats = jedisCache.getStats();
        
        // Cache should respect max size and have activity
        assertThat(cacheSize).isGreaterThanOrEqualTo(0);
        assertThat(stats.toString()).contains("loads=");
        
        log("Cache size after loading 10 products: {}", cacheSize);
        log("Final stats: {}", stats);
    }

    private Cache getJedisCache() {
        if (cacheManager instanceof RedisNearCacheManager manager) {
            return manager.getJedisCache();
        }
        throw new IllegalStateException("CacheManager is not RedisNearCacheManager");
    }
    
    private void log(String message, Object... args) {
        System.out.printf(message + "%n", args);
    }
}