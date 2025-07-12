package com.example.demo.app.service;

import com.example.demo.domain.model.*;
import com.example.demo.infra.cache.CacheAccessTracker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataSimulationService {

    private final ProductService productService;
    private final ProductCatalogService catalogService;
    private final CacheManager cacheManager;
    private final CacheStatisticsService statisticsService;
    private final CacheAccessTracker cacheAccessTracker;
    
    // Keep track of created items for deletion
    private final List<ProductId> createdProductIds = Collections.synchronizedList(new ArrayList<>());
    private final List<String> createdCatalogIds = Collections.synchronizedList(new ArrayList<>());
    
    private final String instanceId = UUID.randomUUID().toString().substring(0, 8);
    
    // Categories for random selection
    private final List<Category> availableCategories = List.of(
        Category.create("electronics", "Electronics", "Electronic devices and gadgets"),
        Category.create("clothing", "Clothing", "Fashion and apparel"),
        Category.create("books", "Books", "Literature and educational materials"),
        Category.create("sports", "Sports", "Sports equipment and gear"),
        Category.create("home", "Home & Garden", "Home improvement and gardening")
    );
    
    // Brands for random selection
    private final List<String> brands = List.of(
        "TechCorp", "StyleBrand", "BookHouse", "SportsPro", "HomeMaster",
        "InnovateInc", "QualityFirst", "PremiumPlus", "EcoFriendly", "ValueMax"
    );

    /**
     * Creates new products every 10-15 seconds with random data
     */
    @Scheduled(fixedDelay = 10000, initialDelay = 5000)
    public void createRandomProducts() {
        try {
            int count = ThreadLocalRandom.current().nextInt(1, 4); // 1-3 products
            
            log.info("[Instance: {}] 🔄 Creating {} random products", instanceId, count);
            
            for (int i = 0; i < count; i++) {
                Product product = generateRandomProduct();
                Product savedProduct = productService.createProduct(product);
                createdProductIds.add(savedProduct.getId());
                
                log.info("[Instance: {}] ✅ Created product: {} (ID: {}, Price: {}, Category: {})", 
                    instanceId, 
                    savedProduct.getDetails().getName(),
                    savedProduct.getId().getValue(),
                    savedProduct.getPrice().getAmount(),
                    savedProduct.getCategory().getId()
                );
                
                // Random delay between products
                if (i < count - 1) {
                    Thread.sleep(ThreadLocalRandom.current().nextInt(500, 2000));
                }
            }
            
            log.info("[Instance: {}] 📊 Total products created so far: {}", instanceId, createdProductIds.size());
            
        } catch (Exception e) {
            log.error("[Instance: {}] ❌ Error creating random products", instanceId, e);
        }
    }

    /**
     * Creates product catalogs every 30-45 seconds
     */
    @Scheduled(fixedDelay = 30000, initialDelay = 15000)
    public void createRandomCatalogs() {
        try {
            String catalogId = "catalog-" + instanceId + "-" + System.currentTimeMillis();
            String catalogName = generateRandomCatalogName();
            
            ProductCatalog catalog = ProductCatalog.create(catalogId, catalogName);
            catalogService.createCatalog(catalog);
            createdCatalogIds.add(catalogId);
            
            log.info("[Instance: {}] 📚 Created catalog: {} (ID: {})", instanceId, catalogName, catalogId);
            
            // Add some random products to the catalog
            if (!createdProductIds.isEmpty()) {
                int productsToAdd = Math.min(3, createdProductIds.size());
                List<ProductId> productsToAddToCatalog = new ArrayList<>(createdProductIds)
                    .subList(Math.max(0, createdProductIds.size() - productsToAdd), createdProductIds.size());
                
                for (ProductId productId : productsToAddToCatalog) {
                    Optional<Product> product = productService.findProductById(productId);
                    if (product.isPresent()) {
                        catalogService.addProductToCatalog(catalogId, product.get());
                        log.info("[Instance: {}] 🔗 Added product {} to catalog {}", 
                            instanceId, productId.getValue(), catalogId);
                    }
                }
            }
            
            log.info("[Instance: {}] 📊 Total catalogs created so far: {}", instanceId, createdCatalogIds.size());
            
        } catch (Exception e) {
            log.error("[Instance: {}] ❌ Error creating random catalog", instanceId, e);
        }
    }

    /**
     * Deletes 1-2 random items every 20-25 seconds
     */
    @Scheduled(fixedDelay = 20000, initialDelay = 25000)
    public void deleteRandomItems() {
        try {
            // Delete products
            if (!createdProductIds.isEmpty() && ThreadLocalRandom.current().nextBoolean()) {
                int productsToDelete = Math.min(2, Math.max(1, createdProductIds.size() / 10));
                
                for (int i = 0; i < productsToDelete && !createdProductIds.isEmpty(); i++) {
                    int randomIndex = ThreadLocalRandom.current().nextInt(createdProductIds.size());
                    ProductId productId = createdProductIds.remove(randomIndex);
                    
                    Optional<Product> product = productService.findProductById(productId);
                    if (product.isPresent()) {
                        productService.deleteProduct(productId);
                        log.info("[Instance: {}] 🗑️ Deleted product: {} (ID: {})", 
                            instanceId, product.get().getDetails().getName(), productId.getValue());
                    } else {
                        log.warn("[Instance: {}] ⚠️ Product {} not found for deletion (may have been deleted by another instance)", 
                            instanceId, productId.getValue());
                    }
                }
            }
            
            // Delete catalogs (less frequently)
            if (!createdCatalogIds.isEmpty() && ThreadLocalRandom.current().nextInt(100) < 30) { // 30% chance
                int randomIndex = ThreadLocalRandom.current().nextInt(createdCatalogIds.size());
                String catalogId = createdCatalogIds.remove(randomIndex);
                
                Optional<ProductCatalog> catalog = catalogService.findCatalogById(catalogId);
                if (catalog.isPresent()) {
                    catalogService.deleteCatalog(catalogId);
                    log.info("[Instance: {}] 🗑️ Deleted catalog: {} (ID: {})", 
                        instanceId, catalog.get().getName(), catalogId);
                } else {
                    log.warn("[Instance: {}] ⚠️ Catalog {} not found for deletion (may have been deleted by another instance)", 
                        instanceId, catalogId);
                }
            }
            
        } catch (Exception e) {
            log.error("[Instance: {}] ❌ Error deleting random items", instanceId, e);
        }
    }

    /**
     * Logs cache statistics every 30 seconds
     */
    @Scheduled(fixedDelay = 30000, initialDelay = 10000)
    public void logCacheStatistics() {
        try {
            // Use the enhanced statistics service for detailed logging
            statisticsService.logDetailedStatistics();
            
            // Also log tracked items
            log.info("[Instance: {}] 📦 Tracked items: {} products, {} catalogs", 
                instanceId, createdProductIds.size(), createdCatalogIds.size());
            
        } catch (Exception e) {
            log.error("[Instance: {}] ❌ Error logging cache statistics", instanceId, e);
        }
    }

    /**
     * Simulates random product access to test cache hits
     */
    @Scheduled(fixedDelay = 8000, initialDelay = 12000)
    public void simulateRandomAccess() {
        try {
            if (!createdProductIds.isEmpty()) {
                int accessCount = ThreadLocalRandom.current().nextInt(2, 6); // 2-5 accesses
                
                log.info("[Instance: {}] 🔍 Simulating {} random product accesses", instanceId, accessCount);
                
                for (int i = 0; i < accessCount; i++) {
                    ProductId randomProductId = createdProductIds.get(
                        ThreadLocalRandom.current().nextInt(createdProductIds.size())
                    );
                    
                    // Track cache access before making the call
                    var cache = cacheManager.getCache("products");
                    if (cache != null) {
                        cacheAccessTracker.trackAccess("products", randomProductId, cache, true);
                    }
                    
                    Optional<Product> product = productService.findProductById(randomProductId);
                    if (product.isPresent()) {
                        log.info("[Instance: {}] 👁️ Accessed product: {} (ID: {})", 
                            instanceId, product.get().getDetails().getName(), randomProductId.getValue());
                    } else {
                        log.warn("[Instance: {}] ⚠️ Product {} not found during access", 
                            instanceId, randomProductId.getValue());
                    }
                    
                    // Small delay between accesses
                    if (i < accessCount - 1) {
                        Thread.sleep(ThreadLocalRandom.current().nextInt(200, 800));
                    }
                }
            }
            
            // Also simulate some catalog accesses
            if (!createdCatalogIds.isEmpty() && ThreadLocalRandom.current().nextBoolean()) {
                String randomCatalogId = createdCatalogIds.get(
                    ThreadLocalRandom.current().nextInt(createdCatalogIds.size())
                );
                
                var cache = cacheManager.getCache("catalogs");
                if (cache != null) {
                    cacheAccessTracker.trackAccess("catalogs", randomCatalogId, cache, true);
                }
                
                Optional<ProductCatalog> catalog = catalogService.findCatalogById(randomCatalogId);
                if (catalog.isPresent()) {
                    log.info("[Instance: {}] 📖 Accessed catalog: {} (ID: {})", 
                        instanceId, catalog.get().getName(), randomCatalogId);
                } else {
                    log.warn("[Instance: {}] ⚠️ Catalog {} not found during access", 
                        instanceId, randomCatalogId);
                }
            }
            
        } catch (Exception e) {
            log.error("[Instance: {}] ❌ Error during random access simulation", instanceId, e);
        }
    }

    private Product generateRandomProduct() {
        String productName = generateRandomProductName();
        String description = "Generated product - " + productName;
        String brand = brands.get(ThreadLocalRandom.current().nextInt(brands.size()));
        String sku = "SKU-" + instanceId + "-" + System.currentTimeMillis();
        
        ProductDetails details = ProductDetails.create(productName, description, brand, sku);
        
        BigDecimal price = BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble(9.99, 999.99))
            .setScale(2, RoundingMode.HALF_UP);
        Price productPrice = Price.of(price.doubleValue(), "USD");
        
        Category category = availableCategories.get(
            ThreadLocalRandom.current().nextInt(availableCategories.size())
        );
        
        return Product.create(details, productPrice, category);
    }

    private String generateRandomProductName() {
        String[] adjectives = {"Premium", "Deluxe", "Professional", "Smart", "Eco", "Ultra", "Advanced", "Classic"};
        String[] nouns = {"Widget", "Device", "Tool", "Gadget", "Item", "Product", "Solution", "System"};
        
        String adjective = adjectives[ThreadLocalRandom.current().nextInt(adjectives.length)];
        String noun = nouns[ThreadLocalRandom.current().nextInt(nouns.length)];
        int number = ThreadLocalRandom.current().nextInt(100, 9999);
        
        return adjective + " " + noun + " " + number;
    }

    private String generateRandomCatalogName() {
        String[] seasons = {"Spring", "Summer", "Fall", "Winter"};
        String[] years = {"2024", "2025"};
        String[] themes = {"Collection", "Series", "Edition", "Catalog", "Selection"};
        
        String season = seasons[ThreadLocalRandom.current().nextInt(seasons.length)];
        String year = years[ThreadLocalRandom.current().nextInt(years.length)];
        String theme = themes[ThreadLocalRandom.current().nextInt(themes.length)];
        
        return season + " " + year + " " + theme;
    }

    public String getInstanceId() {
        return instanceId;
    }
}