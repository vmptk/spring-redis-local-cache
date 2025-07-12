package com.example.demo.app.service;

import java.util.List;
import java.util.Optional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import com.example.demo.domain.model.*;
import com.example.demo.domain.repository.ProductCatalogRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductCatalogService {

    private final ProductCatalogRepository catalogRepository;

    @Cacheable(value = "catalogs", key = "#catalogId")
    public Optional<ProductCatalog> findCatalogById(String catalogId) {
        log.info("Fetching catalog from repository: {}", catalogId);
        return catalogRepository.findById(catalogId);
    }

    @CachePut(value = "catalogs", key = "#catalog.catalogId")
    public ProductCatalog createCatalog(ProductCatalog catalog) {
        log.info("Creating catalog: {}", catalog.getCatalogId());
        return catalogRepository.save(catalog);
    }

    @CachePut(value = "catalogs", key = "#catalog.catalogId")
    public ProductCatalog updateCatalog(ProductCatalog catalog) {
        log.info("Updating catalog: {}", catalog.getCatalogId());
        return catalogRepository.save(catalog);
    }

    @CacheEvict(value = "catalogs", key = "#catalogId")
    public void deleteCatalog(String catalogId) {
        log.info("Deleting catalog: {}", catalogId);
        catalogRepository.deleteById(catalogId);
    }

    @Caching(
            cacheable = @Cacheable(value = "catalogs", key = "#catalogId"),
            put = @CachePut(value = "catalogs", key = "#catalogId", condition = "#result != null"))
    public ProductCatalog addProductToCatalog(String catalogId, Product product) {
        log.info("Adding product {} to catalog {}", product.getId().value(), catalogId);

        Optional<ProductCatalog> catalogOpt = catalogRepository.findById(catalogId);
        if (catalogOpt.isEmpty()) {
            throw new IllegalArgumentException("Catalog not found: " + catalogId);
        }

        ProductCatalog catalog = catalogOpt.get();
        catalog.addProduct(product);
        return catalogRepository.save(catalog);
    }

    @CacheEvict(value = "catalogs", key = "#catalogId")
    public void removeProductFromCatalog(String catalogId, ProductId productId) {
        log.info("Removing product {} from catalog {}", productId.value(), catalogId);

        Optional<ProductCatalog> catalogOpt = catalogRepository.findById(catalogId);
        if (catalogOpt.isEmpty()) {
            throw new IllegalArgumentException("Catalog not found: " + catalogId);
        }

        ProductCatalog catalog = catalogOpt.get();
        catalog.removeProduct(productId);
        catalogRepository.save(catalog);
    }

    public List<ProductCatalog> findAllCatalogs() {
        log.info("Fetching all catalogs");
        return catalogRepository.findAll();
    }

    @CacheEvict(value = "catalogs", allEntries = true)
    public void evictAllCatalogsCache() {
        log.info("Evicting all catalogs from cache");
    }
}
