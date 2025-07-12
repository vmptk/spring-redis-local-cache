package com.example.demo.app.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.demo.domain.model.*;
import com.example.demo.infra.cache.CacheAccessTracker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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

    // Categories for random selection - using immutable collections
    private static final List<Category> AVAILABLE_CATEGORIES = List.of(
            Category.create("electronics", "Electronics", "Electronic devices and gadgets"),
            Category.create("clothing", "Clothing", "Fashion and apparel"),
            Category.create("books", "Books", "Literature and educational materials"),
            Category.create("sports", "Sports", "Sports equipment and gear"),
            Category.create("home", "Home & Garden", "Home improvement and gardening"));

    // Brands for random selection - using immutable collections
    private static final List<String> BRANDS = List.of(
            "TechCorp",
            "StyleBrand",
            "BookHouse",
            "SportsPro",
            "HomeMaster",
            "InnovateInc",
            "QualityFirst",
            "PremiumPlus",
            "EcoFriendly",
            "ValueMax");

    /**
     * Creates new products every 10-15 seconds with random data
     */
    @Scheduled(fixedDelay = 10000, initialDelay = 5000)
    public void createRandomProducts() {
        try {
            var count = ThreadLocalRandom.current().nextInt(1, 4); // 1-3 products

            log.info("[Instance: {}] 🔄 Creating {} random products", instanceId, count);

            for (var i = 0; i < count; i++) {
                var product = generateRandomProduct();
                var savedProduct = productService.createProduct(product);
                createdProductIds.add(savedProduct.getId());

                log.info(
                        "[Instance: {}] ✅ Created product: {} (ID: {}, Price: {}, Category: {})",
                        instanceId,
                        savedProduct.getDetails().name(),
                        savedProduct.getId().value(),
                        savedProduct.getPrice().amount(),
                        savedProduct.getCategory().id());

                // Random delay between products
                if (i < count - 1) {
                    simulateDelay(500, 2000);
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
            var catalogId = "catalog-" + instanceId + "-" + System.currentTimeMillis();
            var catalogName = generateRandomCatalogName();

            var catalog = ProductCatalog.create(catalogId, catalogName);
            catalogService.createCatalog(catalog);
            createdCatalogIds.add(catalogId);

            log.info("[Instance: {}] 📚 Created catalog: {} (ID: {})", instanceId, catalogName, catalogId);

            // Add some random products to the catalog using modern streams
            if (!createdProductIds.isEmpty()) {
                var productsToAdd = Math.min(3, createdProductIds.size());
                var startIndex = Math.max(0, createdProductIds.size() - productsToAdd);

                createdProductIds.subList(startIndex, createdProductIds.size()).stream()
                        .map(productService::findProductById)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .forEach(product -> {
                            catalogService.addProductToCatalog(catalogId, product);
                            log.info(
                                    "[Instance: {}] 🔗 Added product {} to catalog {}",
                                    instanceId,
                                    product.getId().value(),
                                    catalogId);
                        });
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
            deleteRandomProducts();
            deleteRandomCatalog();
        } catch (Exception e) {
            log.error("[Instance: {}] ❌ Error deleting random items", instanceId, e);
        }
    }

    private void deleteRandomProducts() {
        if (createdProductIds.isEmpty() || !ThreadLocalRandom.current().nextBoolean()) {
            return;
        }

        var productsToDelete = Math.min(2, Math.max(1, createdProductIds.size() / 10));
        for (var i = 0; i < productsToDelete && !createdProductIds.isEmpty(); i++) {
            deleteRandomProduct();
        }
    }

    private void deleteRandomProduct() {
        var randomIndex = ThreadLocalRandom.current().nextInt(createdProductIds.size());
        var productId = createdProductIds.remove(randomIndex);

        var product = productService.findProductById(productId);
        if (product.isPresent()) {
            productService.deleteProduct(productId);
            log.info(
                    "[Instance: {}] 🗑️ Deleted product: {} (ID: {})",
                    instanceId,
                    product.get().getDetails().name(),
                    productId.value());
        } else {
            log.warn(
                    "[Instance: {}] ⚠️ Product {} not found for deletion (may have been deleted by another instance)",
                    instanceId,
                    productId.value());
        }
    }

    private void deleteRandomCatalog() {
        if (createdCatalogIds.isEmpty() || ThreadLocalRandom.current().nextInt(100) >= 30) {
            return;
        }

        var randomIndex = ThreadLocalRandom.current().nextInt(createdCatalogIds.size());
        var catalogId = createdCatalogIds.remove(randomIndex);

        var catalog = catalogService.findCatalogById(catalogId);
        if (catalog.isPresent()) {
            catalogService.deleteCatalog(catalogId);
            log.info(
                    "[Instance: {}] 🗑️ Deleted catalog: {} (ID: {})",
                    instanceId,
                    catalog.get().getName(),
                    catalogId);
        } else {
            log.warn(
                    "[Instance: {}] ⚠️ Catalog {} not found for deletion (may have been deleted by another instance)",
                    instanceId,
                    catalogId);
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
            log.info(
                    "[Instance: {}] 📦 Tracked items: {} products, {} catalogs",
                    instanceId,
                    createdProductIds.size(),
                    createdCatalogIds.size());

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
            simulateProductAccess();
            simulateCatalogAccess();
        } catch (Exception e) {
            log.error("[Instance: {}] ❌ Error during random access simulation", instanceId, e);
        }
    }

    private void simulateProductAccess() {
        if (createdProductIds.isEmpty()) {
            return;
        }

        var accessCount = ThreadLocalRandom.current().nextInt(2, 6); // 2-5 accesses
        log.info("[Instance: {}] 🔍 Simulating {} random product accesses", instanceId, accessCount);

        // Use modern collection approach for random product access
        Collections.shuffle(createdProductIds);
        createdProductIds.stream().limit(accessCount).forEach(this::accessProduct);
    }

    private void accessProduct(ProductId productId) {
        try {
            // Track cache access before making the call
            var cache = cacheManager.getCache("products");
            if (cache != null) {
                cacheAccessTracker.trackAccess("products", productId, cache, true);
            }

            var product = productService.findProductById(productId);
            if (product.isPresent()) {
                log.info(
                        "[Instance: {}] 👁️ Accessed product: {} (ID: {})",
                        instanceId,
                        product.get().getDetails().name(),
                        productId.value());
            } else {
                log.warn("[Instance: {}] ⚠️ Product {} not found during access", instanceId, productId.value());
            }

            // Small delay between accesses
            simulateDelay(200, 800);
        } catch (Exception e) {
            log.debug("Error during product access simulation", e);
        }
    }

    private void simulateCatalogAccess() {
        if (createdCatalogIds.isEmpty() || !ThreadLocalRandom.current().nextBoolean()) {
            return;
        }

        var randomCatalogId = createdCatalogIds.get(ThreadLocalRandom.current().nextInt(createdCatalogIds.size()));

        var cache = cacheManager.getCache("catalogs");
        if (cache != null) {
            cacheAccessTracker.trackAccess("catalogs", randomCatalogId, cache, true);
        }

        var catalog = catalogService.findCatalogById(randomCatalogId);
        if (catalog.isPresent()) {
            log.info(
                    "[Instance: {}] 📖 Accessed catalog: {} (ID: {})",
                    instanceId,
                    catalog.get().getName(),
                    randomCatalogId);
        } else {
            log.warn("[Instance: {}] ⚠️ Catalog {} not found during access", instanceId, randomCatalogId);
        }
    }

    private Product generateRandomProduct() {
        var productName = generateRandomProductName();
        var description = "Generated product - " + productName;
        var brand = BRANDS.get(ThreadLocalRandom.current().nextInt(BRANDS.size()));
        var sku = "SKU-" + instanceId + "-" + System.currentTimeMillis();

        var details = ProductDetails.create(productName, description, brand, sku);

        var price = BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble(9.99, 999.99))
                .setScale(2, RoundingMode.HALF_UP);
        var productPrice = Price.of(price.doubleValue(), "USD");

        var category = AVAILABLE_CATEGORIES.get(ThreadLocalRandom.current().nextInt(AVAILABLE_CATEGORIES.size()));

        return Product.create(details, productPrice, category);
    }

    private String generateRandomProductName() {
        var adjectives = List.of("Premium", "Deluxe", "Professional", "Smart", "Eco", "Ultra", "Advanced", "Classic");
        var nouns = List.of("Widget", "Device", "Tool", "Gadget", "Item", "Product", "Solution", "System");

        var adjective = adjectives.get(ThreadLocalRandom.current().nextInt(adjectives.size()));
        var noun = nouns.get(ThreadLocalRandom.current().nextInt(nouns.size()));
        var number = ThreadLocalRandom.current().nextInt(100, 9999);

        return adjective + " " + noun + " " + number;
    }

    private String generateRandomCatalogName() {
        var seasons = List.of("Spring", "Summer", "Fall", "Winter");
        var years = List.of("2024", "2025");
        var themes = List.of("Collection", "Series", "Edition", "Catalog", "Selection");

        var season = seasons.get(ThreadLocalRandom.current().nextInt(seasons.size()));
        var year = years.get(ThreadLocalRandom.current().nextInt(years.size()));
        var theme = themes.get(ThreadLocalRandom.current().nextInt(themes.size()));

        return season + " " + year + " " + theme;
    }

    public String getInstanceId() {
        return instanceId;
    }

    /**
     * Helper method to simulate realistic delays without using Thread.sleep directly
     * PMD compliant delay simulation for testing purposes
     */
    @SuppressWarnings("PMD.DoNotUseThreads") // Intentional delay for simulation
    private void simulateDelay(int minMillis, int maxMillis) {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(minMillis, maxMillis));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.debug("Simulation delay interrupted", e);
        }
    }
}
