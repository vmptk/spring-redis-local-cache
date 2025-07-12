package com.example.demo.app.service;

import java.util.List;
import java.util.Optional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.example.demo.domain.model.Price;
import com.example.demo.domain.model.Product;
import com.example.demo.domain.model.ProductId;
import com.example.demo.domain.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Cacheable(value = "products", key = "#productId.value")
    public Optional<Product> findProductById(ProductId productId) {
        log.info("Fetching product from repository: {}", productId.value());
        return productRepository.findById(productId);
    }

    @CachePut(value = "products", key = "#product.id.value")
    public Product createProduct(Product product) {
        log.info("Creating product: {}", product.getId().value());
        return productRepository.save(product);
    }

    @CachePut(value = "products", key = "#product.id.value")
    public Product updateProduct(Product product) {
        log.info("Updating product: {}", product.getId().value());
        return productRepository.save(product);
    }

    @CacheEvict(value = "products", key = "#productId.value")
    public void deleteProduct(ProductId productId) {
        log.info("Deleting product: {}", productId.value());
        productRepository.deleteById(productId);
    }

    @CachePut(value = "products", key = "#productId.value")
    public Product updateProductPrice(ProductId productId, Price newPrice) {
        log.info("Updating price for product: {}", productId.value());

        var productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) {
            throw new IllegalArgumentException("Product not found: " + productId.value());
        }

        var product = productOpt.get();
        product.updatePrice(newPrice);
        return productRepository.save(product);
    }

    @Cacheable(value = "products", key = "'category:' + #categoryId")
    public List<Product> findProductsByCategory(String categoryId) {
        log.info("Fetching products by category: {}", categoryId);
        return productRepository.findByCategory(categoryId);
    }

    @Cacheable(value = "products", key = "'active'")
    public List<Product> findActiveProducts() {
        log.info("Fetching active products");
        return productRepository.findActiveProducts();
    }

    public List<Product> findAllProducts() {
        log.info("Fetching all products");
        return productRepository.findAll();
    }

    @CacheEvict(value = "products", allEntries = true)
    public void evictAllProductsCache() {
        log.info("Evicting all products from cache");
    }

    @CachePut(value = "products", key = "#productId.value")
    public Product activateProduct(ProductId productId) {
        log.info("Activating product: {}", productId.value());

        var productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) {
            throw new IllegalArgumentException("Product not found: " + productId.value());
        }

        var product = productOpt.get();
        product.activate();
        return productRepository.save(product);
    }

    @CachePut(value = "products", key = "#productId.value")
    public Product deactivateProduct(ProductId productId) {
        log.info("Deactivating product: {}", productId.value());

        var productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) {
            throw new IllegalArgumentException("Product not found: " + productId.value());
        }

        var product = productOpt.get();
        product.deactivate();
        return productRepository.save(product);
    }
}
