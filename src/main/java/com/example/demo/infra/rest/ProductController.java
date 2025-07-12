package com.example.demo.infra.rest;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.app.controller.dto.CreateProductRequest;
import com.example.demo.app.controller.dto.UpdatePriceRequest;
import com.example.demo.app.service.ProductService;
import com.example.demo.domain.model.*;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody CreateProductRequest request) {
        var details = ProductDetails.create(request.name(), request.description(), request.brand(), request.sku());

        var price = Price.of(request.price(), request.currency());

        var category = Category.create(request.categoryId(), request.categoryName(), request.categoryDescription());

        var product = Product.create(details, price, category);
        var savedProduct = productService.createProduct(product);

        return ResponseEntity.status(HttpStatus.CREATED).body(savedProduct);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<Product> getProduct(@PathVariable String productId) {
        return productService
                .findProductById(ProductId.of(productId))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{productId}/price")
    public ResponseEntity<Product> updatePrice(
            @PathVariable String productId, @RequestBody UpdatePriceRequest request) {
        var newPrice = Price.of(request.price(), request.currency());
        var updatedProduct = productService.updateProductPrice(ProductId.of(productId), newPrice);
        return ResponseEntity.ok(updatedProduct);
    }

    @PutMapping("/{productId}/activate")
    public ResponseEntity<Product> activateProduct(@PathVariable String productId) {
        var product = productService.activateProduct(ProductId.of(productId));
        return ResponseEntity.ok(product);
    }

    @PutMapping("/{productId}/deactivate")
    public ResponseEntity<Product> deactivateProduct(@PathVariable String productId) {
        var product = productService.deactivateProduct(ProductId.of(productId));
        return ResponseEntity.ok(product);
    }

    @GetMapping("/category/{categoryId}")
    public List<Product> getProductsByCategory(@PathVariable String categoryId) {
        return productService.findProductsByCategory(categoryId);
    }

    @GetMapping("/active")
    public List<Product> getActiveProducts() {
        return productService.findActiveProducts();
    }

    @GetMapping
    public List<Product> getAllProducts() {
        return productService.findAllProducts();
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String productId) {
        productService.deleteProduct(ProductId.of(productId));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/cache/evict")
    public ResponseEntity<String> evictCache() {
        productService.evictAllProductsCache();
        return ResponseEntity.ok("Product cache evicted successfully");
    }
}
