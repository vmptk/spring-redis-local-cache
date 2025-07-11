package com.example.demo.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductCatalog implements Serializable {
    private String catalogId;
    private String name;
    private Map<ProductId, Product> products;
    private LocalDateTime lastUpdated;

    @JsonCreator
    public ProductCatalog(@JsonProperty("catalogId") String catalogId,
                         @JsonProperty("name") String name,
                         @JsonProperty("products") Map<ProductId, Product> products,
                         @JsonProperty("lastUpdated") LocalDateTime lastUpdated) {
        this.catalogId = catalogId;
        this.name = name;
        this.products = products != null ? products : new HashMap<>();
        this.lastUpdated = lastUpdated;
    }

    public static ProductCatalog create(String catalogId, String name) {
        if (catalogId == null || catalogId.trim().isEmpty()) {
            throw new IllegalArgumentException("Catalog ID cannot be null or empty");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Catalog name cannot be null or empty");
        }
        return new ProductCatalog(
                catalogId,
                name,
                new HashMap<>(),
                LocalDateTime.now()
        );
    }

    public void addProduct(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("Product cannot be null");
        }
        products.put(product.getId(), product);
        lastUpdated = LocalDateTime.now();
    }

    public void removeProduct(ProductId productId) {
        if (productId == null) {
            throw new IllegalArgumentException("Product ID cannot be null");
        }
        products.remove(productId);
        lastUpdated = LocalDateTime.now();
    }

    public Optional<Product> findProduct(ProductId productId) {
        return Optional.ofNullable(products.get(productId));
    }

    public List<Product> findProductsByCategory(String categoryId) {
        return products.values().stream()
                .filter(p -> p.getCategory().getId().equals(categoryId))
                .collect(Collectors.toList());
    }

    public List<Product> getActiveProducts() {
        return products.values().stream()
                .filter(Product::isActive)
                .collect(Collectors.toList());
    }

    public int getProductCount() {
        return products.size();
    }

    public int getActiveProductCount() {
        return (int) products.values().stream()
                .filter(Product::isActive)
                .count();
    }
}