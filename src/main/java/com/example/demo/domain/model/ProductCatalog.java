package com.example.demo.domain.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductCatalog implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String catalogId;
    private final String name;

    @JsonIgnore
    private final Map<ProductId, Product> products;

    // For JSON serialization - store products as a list
    @JsonProperty("productList")
    private List<Product> productList;

    private LocalDateTime lastUpdated;

    // Protected no-args constructor for frameworks
    protected ProductCatalog() {
        this.catalogId = null;
        this.name = null;
        this.products = new HashMap<>();
        this.productList = new ArrayList<>();
        this.lastUpdated = LocalDateTime.now();
    }

    @JsonCreator
    public ProductCatalog(
            @JsonProperty("catalogId") String catalogId,
            @JsonProperty("name") String name,
            @JsonProperty("productList") List<Product> productList,
            @JsonProperty("lastUpdated") LocalDateTime lastUpdated) {
        this.catalogId = catalogId;
        this.name = name;
        this.lastUpdated = lastUpdated;

        // Initialize products map from the list
        this.products = new HashMap<>();
        this.productList = productList != null ? productList : new ArrayList<>();

        if (productList != null) {
            for (Product product : productList) {
                this.products.put(product.getId(), product);
            }
        }
    }

    public static ProductCatalog create(String catalogId, String name) {
        if (catalogId == null || catalogId.isBlank()) {
            throw new IllegalArgumentException("Catalog ID cannot be null or empty");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Catalog name cannot be null or empty");
        }
        return new ProductCatalog(catalogId, name, new ArrayList<>(), LocalDateTime.now());
    }

    public void addProduct(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("Product cannot be null");
        }
        products.put(product.getId(), product);
        syncProductList();
        lastUpdated = LocalDateTime.now();
    }

    public void removeProduct(ProductId productId) {
        if (productId == null) {
            throw new IllegalArgumentException("Product ID cannot be null");
        }
        products.remove(productId);
        syncProductList();
        lastUpdated = LocalDateTime.now();
    }

    // Helper method to keep productList in sync with products map
    private void syncProductList() {
        this.productList = new ArrayList<>(products.values());
    }

    public Optional<Product> findProduct(ProductId productId) {
        return Optional.ofNullable(products.get(productId));
    }

    public List<Product> findProductsByCategory(String categoryId) {
        return products.values().stream()
                .filter(p -> p.getCategory().id().equals(categoryId))
                .toList();
    }

    @JsonIgnore
    public List<Product> getActiveProducts() {
        return products.values().stream().filter(Product::isActive).toList();
    }

    public int getProductCount() {
        return products.size();
    }

    @JsonIgnore
    public int getActiveProductCount() {
        return (int) products.values().stream().filter(Product::isActive).count();
    }
}
