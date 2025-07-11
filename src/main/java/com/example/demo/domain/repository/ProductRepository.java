package com.example.demo.domain.repository;

import com.example.demo.domain.model.Product;
import com.example.demo.domain.model.ProductId;
import java.util.Optional;
import java.util.List;

public interface ProductRepository {
    Product save(Product product);
    Optional<Product> findById(ProductId productId);
    List<Product> findByCategory(String categoryId);
    List<Product> findAll();
    void deleteById(ProductId productId);
    boolean existsById(ProductId productId);
    List<Product> findActiveProducts();
}