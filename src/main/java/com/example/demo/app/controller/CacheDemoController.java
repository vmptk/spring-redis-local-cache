package com.example.demo.app.controller;

import com.example.demo.app.service.ProductService;
import com.example.demo.domain.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/demo")
@RequiredArgsConstructor
public class CacheDemoController {
    
    private final ProductService productService;

    @PostMapping("/create-sample-data")
    public Map<String, Object> createSampleData() {
        // Create sample products
        Category electronics = Category.create("electronics", "Electronics", "Electronic products");
        
        Product laptop = Product.create(
                ProductDetails.create("Gaming Laptop", "High-performance gaming laptop", "TechBrand", "LAPTOP-001"),
                Price.of(1499.99, "USD"),
                electronics
        );
        
        Product phone = Product.create(
                ProductDetails.create("Smartphone", "Latest smartphone with advanced features", "PhoneBrand", "PHONE-001"),
                Price.of(899.99, "USD"),
                electronics
        );
        
        Product savedLaptop = productService.createProduct(laptop);
        Product savedPhone = productService.createProduct(phone);
        
        Map<String, Object> result = new HashMap<>();
        result.put("laptop", Map.of("id", savedLaptop.getId().getValue(), "name", savedLaptop.getDetails().getName()));
        result.put("phone", Map.of("id", savedPhone.getId().getValue(), "name", savedPhone.getDetails().getName()));
        result.put("message", "Sample products created successfully");
        
        return result;
    }

    @GetMapping("/cache-performance-test/{productId}")
    public Map<String, Object> testCachePerformance(@PathVariable String productId) {
        Map<String, Object> result = new HashMap<>();
        
        // First access (cold cache)
        Instant start = Instant.now();
        Product product1 = productService.findProductById(ProductId.of(productId)).orElse(null);
        Duration firstAccess = Duration.between(start, Instant.now());
        
        // Second access (warm cache)
        start = Instant.now();
        Product product2 = productService.findProductById(ProductId.of(productId)).orElse(null);
        Duration secondAccess = Duration.between(start, Instant.now());
        
        // Third access (warm cache)
        start = Instant.now();
        Product product3 = productService.findProductById(ProductId.of(productId)).orElse(null);
        Duration thirdAccess = Duration.between(start, Instant.now());
        
        if (product1 != null) {
            result.put("productName", product1.getDetails().getName());
            result.put("firstAccessTime", firstAccess.toMillis() + "ms");
            result.put("secondAccessTime", secondAccess.toMillis() + "ms");
            result.put("thirdAccessTime", thirdAccess.toMillis() + "ms");
            
            double improvement = ((double) firstAccess.toMillis() - secondAccess.toMillis()) / firstAccess.toMillis() * 100;
            result.put("cachePerformanceImprovement", String.format("%.2f%%", improvement));
            result.put("message", "Cache performance test completed - lower times indicate cache hits");
        } else {
            result.put("error", "Product not found");
        }
        
        return result;
    }

    @PostMapping("/evict-cache")
    public Map<String, String> evictCache() {
        productService.evictAllProductsCache();
        return Map.of("message", "All product caches evicted successfully");
    }
}