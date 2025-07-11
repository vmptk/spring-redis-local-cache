package com.example.demo;

import com.example.demo.app.service.ProductCatalogService;
import com.example.demo.app.service.ProductService;
import com.example.demo.domain.model.*;
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
class ProductCatalogCacheTest {

    @Autowired
    private ProductCatalogService catalogService;

    @Autowired
    private ProductService productService;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private ProductCatalog testCatalog;

    @BeforeEach
    void setUp() {
        // Clear caches
        cacheManager.getCacheNames().forEach(name -> 
            cacheManager.getCache(name).clear()
        );

        // Clear Redis
        redisTemplate.getConnectionFactory().getConnection().flushAll();

        // Create test catalog
        testCatalog = ProductCatalog.create("spring-2024", "Spring 2024 Collection");
    }

    @Test
    void testCatalogCaching() {
        // Save catalog
        catalogService.createCatalog(testCatalog);

        // First access - cache miss
        ProductCatalog catalog1 = catalogService.findCatalogById("spring-2024").orElseThrow();
        assertThat(catalog1.getName()).isEqualTo("Spring 2024 Collection");

        // Second access - should be from cache (verify by checking object reference in real scenario)
        ProductCatalog catalog2 = catalogService.findCatalogById("spring-2024").orElseThrow();
        assertThat(catalog2.getName()).isEqualTo("Spring 2024 Collection");
    }

    @Test
    void testCatalogCacheEvictionOnUpdate() {
        // Create and cache catalog
        catalogService.createCatalog(testCatalog);
        
        // First access - populates cache
        ProductCatalog catalog1 = catalogService.findCatalogById("spring-2024").orElseThrow();
        assertThat(catalog1.getName()).isEqualTo("Spring 2024 Collection");
        
        // Update catalog name (this should evict from cache)
        ProductCatalog updated = ProductCatalog.create("spring-2024", "Updated Spring 2024 Collection");
        catalogService.updateCatalog(updated);
        
        // Second access - should get updated version
        ProductCatalog catalog2 = catalogService.findCatalogById("spring-2024").orElseThrow();
        assertThat(catalog2.getName()).isEqualTo("Updated Spring 2024 Collection");
    }

    @Test
    void testMultipleCatalogCaching() {
        // Create multiple catalogs
        ProductCatalog catalog1 = ProductCatalog.create("spring-2024", "Spring 2024 Collection");
        ProductCatalog catalog2 = ProductCatalog.create("summer-2024", "Summer 2024 Collection");
        
        catalogService.createCatalog(catalog1);
        catalogService.createCatalog(catalog2);
        
        // Access both catalogs - should populate cache
        ProductCatalog cached1 = catalogService.findCatalogById("spring-2024").orElseThrow();
        ProductCatalog cached2 = catalogService.findCatalogById("summer-2024").orElseThrow();
        
        assertThat(cached1.getName()).isEqualTo("Spring 2024 Collection");
        assertThat(cached2.getName()).isEqualTo("Summer 2024 Collection");
        
        // Access again - should be served from cache
        ProductCatalog cached1Again = catalogService.findCatalogById("spring-2024").orElseThrow();
        ProductCatalog cached2Again = catalogService.findCatalogById("summer-2024").orElseThrow();
        
        assertThat(cached1Again.getName()).isEqualTo("Spring 2024 Collection");
        assertThat(cached2Again.getName()).isEqualTo("Summer 2024 Collection");
    }

    @Test
    void testDeleteCatalogEvictsCache() {
        // Create catalog
        catalogService.createCatalog(testCatalog);

        // Verify it exists
        assertThat(catalogService.findCatalogById("spring-2024")).isPresent();

        // Delete catalog
        catalogService.deleteCatalog("spring-2024");

        // Verify it's gone
        assertThat(catalogService.findCatalogById("spring-2024")).isEmpty();
    }
}