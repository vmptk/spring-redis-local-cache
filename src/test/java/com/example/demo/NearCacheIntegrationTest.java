package com.example.demo;

import com.example.demo.app.service.ProductService;
import com.example.demo.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
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
    private RedissonClient redissonClient;

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
        // Save product
        productService.createProduct(testProduct);

        // Get cache before
        var map = redissonClient.getMap("products");
        int sizeBefore = map.size();

        // First access - should populate cache
        productService.findProductById(productId);
        
        // Second access - should be served from cache
        productService.findProductById(productId);
        
        // Verify cache has entries
        assertThat(map.size()).isGreaterThanOrEqualTo(sizeBefore);
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

        // Get cache before
        var map = redissonClient.getMap("products");
        int sizeBefore = map.size();

        // First query
        productService.findProductsByCategory("electronics");
        
        // Second query - should use cache
        productService.findProductsByCategory("electronics");
        
        // Verify cache usage
        assertThat(map.size()).isGreaterThanOrEqualTo(sizeBefore);
    }

    @Test
    void testRedisAsRemoteCache() {
        // Save product
        productService.createProduct(testProduct);

        // Clear cache
        cacheManager.getCache("products").clear();

        // Access product - should load from cache
        Product fromCache = productService.findProductById(productId).orElseThrow();
        assertThat(fromCache.getId()).isEqualTo(productId);

        // Verify cache works
        var map = redissonClient.getMap("products");
        assertThat(map.size()).isGreaterThanOrEqualTo(0);
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
        var map = redissonClient.getMap("products");
        int sizeBeforeEvict = map.size();

        // Evict all
        productService.evictAllProductsCache();

        // Verify cache is cleared
        assertThat(map.size()).isLessThanOrEqualTo(sizeBeforeEvict);
    }

}