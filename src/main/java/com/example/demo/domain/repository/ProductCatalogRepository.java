package com.example.demo.domain.repository;

import com.example.demo.domain.model.ProductCatalog;
import java.util.Optional;
import java.util.List;

public interface ProductCatalogRepository {
    ProductCatalog save(ProductCatalog catalog);
    Optional<ProductCatalog> findById(String catalogId);
    List<ProductCatalog> findAll();
    void deleteById(String catalogId);
    boolean existsById(String catalogId);
}