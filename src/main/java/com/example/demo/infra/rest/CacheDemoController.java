package com.example.demo.infra.rest;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.*;

import com.example.demo.app.service.ProductService;
import com.example.demo.domain.model.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/demo")
@RequiredArgsConstructor
public class CacheDemoController {

    private final ProductService productService;

    @PostMapping("/create-sample-data")
    public Map<String, Object> createSampleData() {
        // Create sample products
        var electronics = Category.create("electronics", "Electronics", "Electronic products");

        var laptop = Product.create(
                ProductDetails.create("Gaming Laptop", "High-performance gaming laptop", "TechBrand", "LAPTOP-001"),
                Price.of(1499.99, "USD"),
                electronics);

        var phone = Product.create(
                ProductDetails.create(
                        "Smartphone", "Latest smartphone with advanced features", "PhoneBrand", "PHONE-001"),
                Price.of(899.99, "USD"),
                electronics);

        var savedLaptop = productService.createProduct(laptop);
        var savedPhone = productService.createProduct(phone);

        var result = new HashMap<String, Object>();
        result.put(
                "laptop",
                Map.of(
                        "id",
                        savedLaptop.getId().value(),
                        "name",
                        savedLaptop.getDetails().name()));
        result.put(
                "phone",
                Map.of(
                        "id",
                        savedPhone.getId().value(),
                        "name",
                        savedPhone.getDetails().name()));
        result.put("message", "Sample products created successfully");

        return result;
    }

    @GetMapping("/cache-performance-test/{productId}")
    public Map<String, Object> testCachePerformance(@PathVariable String productId) {
        var result = new HashMap<String, Object>();

        // First access (cold cache)
        var start = Instant.now();
        var product1 = productService.findProductById(ProductId.of(productId)).orElse(null);
        var firstAccess = Duration.between(start, Instant.now());

        // Second access (warm cache)
        start = Instant.now();
        productService.findProductById(ProductId.of(productId));
        var secondAccess = Duration.between(start, Instant.now());

        // Third access (warm cache)
        start = Instant.now();
        productService.findProductById(ProductId.of(productId));
        var thirdAccess = Duration.between(start, Instant.now());

        if (product1 != null) {
            result.put("productName", product1.getDetails().name());
            result.put("firstAccessTime", firstAccess.toMillis() + "ms");
            result.put("secondAccessTime", secondAccess.toMillis() + "ms");
            result.put("thirdAccessTime", thirdAccess.toMillis() + "ms");

            var improvement =
                    ((double) firstAccess.toMillis() - secondAccess.toMillis()) / firstAccess.toMillis() * 100;
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
