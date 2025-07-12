package com.example.demo.domain.repository;

import java.util.List;
import java.util.Optional;

import com.example.demo.domain.model.ProductCatalog;

public interface ProductCatalogRepository {
    ProductCatalog save(ProductCatalog catalog);

    Optional<ProductCatalog> findById(String catalogId);

    List<ProductCatalog> findAll();

    void deleteById(String catalogId);

    boolean existsById(String catalogId);
}
