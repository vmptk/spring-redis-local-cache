package com.example.demo.infra.rest;

import com.example.demo.app.controller.dto.CreateCatalogRequest;
import com.example.demo.app.service.ProductCatalogService;
import com.example.demo.app.service.ProductService;
import com.example.demo.domain.model.Product;
import com.example.demo.domain.model.ProductCatalog;
import com.example.demo.domain.model.ProductId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalogs")
@RequiredArgsConstructor
public class ProductCatalogController {
    
    private final ProductCatalogService catalogService;
    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductCatalog> createCatalog(@RequestBody CreateCatalogRequest request) {
        ProductCatalog catalog = ProductCatalog.create(request.getCatalogId(), request.getName());
        ProductCatalog savedCatalog = catalogService.createCatalog(catalog);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedCatalog);
    }

    @GetMapping("/{catalogId}")
    public ResponseEntity<ProductCatalog> getCatalog(@PathVariable String catalogId) {
        return catalogService.findCatalogById(catalogId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{catalogId}/products/{productId}")
    public ResponseEntity<ProductCatalog> addProductToCatalog(@PathVariable String catalogId,
                                                            @PathVariable String productId) {
        Product product = productService.findProductById(ProductId.of(productId))
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        
        ProductCatalog updatedCatalog = catalogService.addProductToCatalog(catalogId, product);
        return ResponseEntity.ok(updatedCatalog);
    }

    @DeleteMapping("/{catalogId}/products/{productId}")
    public ResponseEntity<Void> removeProductFromCatalog(@PathVariable String catalogId,
                                                       @PathVariable String productId) {
        catalogService.removeProductFromCatalog(catalogId, ProductId.of(productId));
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public List<ProductCatalog> getAllCatalogs() {
        return catalogService.findAllCatalogs();
    }

    @DeleteMapping("/{catalogId}")
    public ResponseEntity<Void> deleteCatalog(@PathVariable String catalogId) {
        catalogService.deleteCatalog(catalogId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/cache/evict")
    public ResponseEntity<String> evictCache() {
        catalogService.evictAllCatalogsCache();
        return ResponseEntity.ok("Catalog cache evicted successfully");
    }
}