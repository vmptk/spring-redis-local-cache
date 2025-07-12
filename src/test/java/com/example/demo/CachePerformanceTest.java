package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;

import com.example.demo.app.service.ProductService;
import com.example.demo.domain.model.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class CachePerformanceTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private List<Product> testProducts;

    @BeforeEach
    void setUp() {
        // Clear caches
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());

        // Clear Redis
        redisTemplate.getConnectionFactory().getConnection().flushAll();

        // Create test products
        testProducts = new ArrayList<>();
        Category electronics = Category.create("electronics", "Electronics", "Electronic products");

        for (int i = 0; i < 100; i++) {
            ProductDetails details = ProductDetails.create(
                    "Product " + i, "Description " + i, "Brand " + (i % 5), "SKU-" + String.format("%03d", i));
            Price price = Price.of(99.99 + i, "USD");
            Product product = Product.create(details, price, electronics);
            testProducts.add(productService.createProduct(product));
        }
    }

    @Test
    @org.junit.jupiter.api.Disabled("Performance test can be flaky in test environment")
    void testCachePerformanceImprovement() {
        // Measure time without cache (first access)
        var start = Instant.now();
        testProducts.forEach(product -> productService.findProductById(product.getId()));
        var firstAccessDuration = Duration.between(start, Instant.now());
        System.out.println("First access (no cache) duration: " + firstAccessDuration.toMillis() + "ms");

        // Measure time with cache (second access)
        start = Instant.now();
        testProducts.forEach(product -> productService.findProductById(product.getId()));
        var secondAccessDuration = Duration.between(start, Instant.now());
        System.out.println("Second access (with cache) duration: " + secondAccessDuration.toMillis() + "ms");

        // Cache should be significantly faster
        assertThat(secondAccessDuration).isLessThan(firstAccessDuration.dividedBy(2));

        // Calculate improvement
        double improvement = ((double) firstAccessDuration.toMillis() - secondAccessDuration.toMillis())
                / firstAccessDuration.toMillis()
                * 100;
        System.out.println("Performance improvement: " + String.format("%.2f", improvement) + "%");
    }

    @Test
    @org.junit.jupiter.api.Disabled("Concurrent test can be flaky in test environment")
    void testConcurrentCacheAccess() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(10);

        // Clear cache
        cacheManager.getCache("products").clear();

        // Concurrent access to same products
        var futures = new ArrayList<CompletableFuture<Void>>();
        var start = Instant.now();

        for (int thread = 0; thread < 10; thread++) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(
                    () -> {
                        // Each thread accesses first 20 products 5 times
                        for (int iteration = 0; iteration < 5; iteration++) {
                            for (int i = 0; i < 20; i++) {
                                productService.findProductById(
                                        testProducts.get(i).getId());
                            }
                        }
                    },
                    executor);
            futures.add(future);
        }

        // Wait for all threads to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        var duration = Duration.between(start, Instant.now());

        System.out.println("Concurrent access duration: " + duration.toMillis() + "ms");
        System.out.println("Total operations: " + (10 * 5 * 20) + " product lookups");
        System.out.println("Operations per second: " + (1000 / (duration.toMillis() / 1000.0)));

        executor.shutdown();
    }

    @Test
    @org.junit.jupiter.api.Disabled("Performance test can be flaky in test environment")
    void testCacheSizeImpact() {
        // Test with increasing number of cached items
        var durations = new ArrayList<Duration>();

        for (int size : List.of(10, 50, 100)) {
            // Clear cache
            cacheManager.getCache("products").clear();

            // Populate cache with specific number of items
            IntStream.range(0, size)
                    .forEach(i ->
                            productService.findProductById(testProducts.get(i).getId()));

            // Measure access time for cached items
            var start = Instant.now();
            IntStream.range(0, size)
                    .forEach(i ->
                            productService.findProductById(testProducts.get(i).getId()));
            var duration = Duration.between(start, Instant.now());

            durations.add(duration);
            System.out.println("Access time for " + size + " cached items: " + duration.toMillis() + "ms");
        }

        // Access time should remain relatively constant regardless of cache size
        assertThat(durations.get(2).toMillis()).isLessThan(durations.get(0).toMillis() * 2);
    }

    @Test
    @org.junit.jupiter.api.Disabled("Performance test can be flaky in test environment")
    void testCategoryQueryPerformance() {
        // Clear cache
        cacheManager.getCache("products").clear();

        // First query - no cache
        var start = Instant.now();
        List<Product> products1 = productService.findProductsByCategory("electronics");
        var firstQueryDuration = Duration.between(start, Instant.now());
        System.out.println("First category query duration: " + firstQueryDuration.toMillis() + "ms");
        System.out.println("Products found: " + products1.size());

        // Second query - with cache
        start = Instant.now();
        List<Product> products2 = productService.findProductsByCategory("electronics");
        var secondQueryDuration = Duration.between(start, Instant.now());
        System.out.println("Second category query duration: " + secondQueryDuration.toMillis() + "ms");

        // Cache should provide significant speedup
        assertThat(secondQueryDuration).isLessThan(firstQueryDuration.dividedBy(5));
        assertThat(products2).hasSize(products1.size());
    }
}
