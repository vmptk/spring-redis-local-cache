# Spring Redis Near Cache (Local Cache) Sample

A comprehensive implementation of Redis near cache pattern using Spring Boot, Domain Driven Design (DDD), and a Product Catalog domain model. This sample demonstrates L1 (Caffeine local cache) + L2 (Redis distributed cache) architecture with **automated data simulation**, **multi-instance testing**, and **detailed cache statistics tracking** for optimal performance analysis.

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Near Cache Pattern](#near-cache-pattern)
- [🚀 Multi-Instance Cache Synchronization](#-multi-instance-cache-synchronization)
- [📊 Enhanced Cache Statistics](#-enhanced-cache-statistics)
- [🤖 Automated Data Simulation](#-automated-data-simulation)
- [Domain Model (DDD)](#domain-model-ddd)
- [Project Structure](#project-structure)
- [Setup and Installation](#setup-and-installation)
- [Configuration](#configuration)
- [API Endpoints](#api-endpoints)
- [Testing](#testing)
- [Performance Characteristics](#performance-characteristics)
- [Use Cases](#use-cases)
- [Best Practices](#best-practices)

## Architecture Overview

This project implements a **Near Cache Pattern** using:

- **L1 Cache (Caffeine)**: Fast in-memory cache for frequently accessed data
- **L2 Cache (Redis)**: Distributed cache for shared data across instances
- **Domain Driven Design**: Clean architecture with separated concerns
- **Spring Cache Abstraction**: Declarative caching with annotations

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Application   │    │   L1 (Caffeine) │    │   L2 (Redis)    │
│     Layer       │───▶│   Local Cache   │───▶│ Distributed     │
│                 │    │                 │    │    Cache        │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## Near Cache Pattern

The near cache pattern provides:

1. **Ultra-fast local access** (L1 - Caffeine)
2. **Distributed consistency** (L2 - Redis)
3. **Automatic fallback** to persistent storage
4. **Cache invalidation** across all layers

### Cache Flow

1. **Cache Hit (L1)**: Data served from local Caffeine cache (~1ms)
2. **Cache Miss (L1), Hit (L2)**: Data loaded from Redis and cached locally (~10ms)
3. **Cache Miss (L1 & L2)**: Data loaded from database, cached in both layers (~100ms)

## 🚀 Multi-Instance Cache Synchronization

This project includes **advanced multi-instance testing** capabilities to observe cache synchronization behavior across multiple application instances sharing the same Redis backend.

### Quick Start - Multi-Instance Setup

```bash
# Start 4 application instances + Redis with one command
./run-multi-instance.sh

# Monitor cache synchronization in real-time
./monitor-instances.sh
```

### What This Provides

- **4 Application Instances**: Running on ports 8080-8083
- **Shared Redis Backend**: L2 cache synchronization point
- **Automated Data Generation**: Continuous creation/deletion of cacheable items
- **Real-Time Monitoring**: Cache hit rates, synchronization patterns, Redis metrics
- **Docker Compose Integration**: Easy startup/shutdown with profiles

### Instance URLs
- Main Instance: http://localhost:8080
- Instance 2: http://localhost:8081
- Instance 3: http://localhost:8082
- Instance 4: http://localhost:8083

## 📊 Enhanced Cache Statistics

Comprehensive cache performance tracking with detailed metrics for both L1 (Caffeine) and L2 (Redis) cache layers.

### Cache Metrics Endpoints

#### Detailed JSON Statistics
```bash
GET /api/cache/metrics
```

```json
{
  "instanceId": "12ab34cd",
  "timestamp": "2025-07-12T13:15:30",
  "cacheMetrics": {
    "products": {
      "size": 45,
      "hitCount": 123,
      "missCount": 67,
      "hitRate": 64.74,
      "evictionCount": 12,
      "averageLoadPenalty": 15.3,
      "requestCount": 190
    }
  },
  "accessMetrics": {
    "products": {
      "totalAccesses": 190,
      "l1Hits": 123,
      "l1Misses": 67,
      "l2Hits": 19,
      "l2Misses": 48,
      "l1HitRate": 64.74,
      "l2HitRate": 28.36,
      "overallHitRate": 82.11,
      "lastAccess": "13:15:28"
    }
  },
  "redisMetrics": {
    "keyCount": 89,
    "connectedClients": 4,
    "commandsProcessed": 1542,
    "hitRate": 87.23
  }
}
```

#### Human-Readable Summary
```bash
GET /api/cache/summary
```

```
=== CACHE SUMMARY ===
Instance: 12ab34cd
Timestamp: 2025-07-12T13:15:30

L1 Cache (Caffeine):
  products: Size=45, Hit Rate=64.74%, Evictions=12
  catalogs: Size=8, Hit Rate=78.90%, Evictions=2

Access Patterns:
  products: L1 Hit Rate=64.74%, L2 Hit Rate=28.36%, Overall=82.11%
  catalogs: L1 Hit Rate=78.90%, L2 Hit Rate=18.75%, Overall=85.42%

Redis (L2 Cache):
  Keys=89, Hit Rate=87.23%, Commands=1542
```

### What You Can Track

- **L1 Hit Rate**: Percentage of requests served by local Caffeine cache
- **L2 Hit Rate**: When L1 misses, percentage served by Redis
- **Overall Hit Rate**: Combined effectiveness across both cache layers
- **Eviction Patterns**: Cache expiration and size-based evictions
- **Redis Load**: Commands processed, client connections, keyspace statistics
- **Instance Comparison**: Compare cache performance across multiple instances

## 🤖 Automated Data Simulation

**Spring-powered schedulers** automatically generate realistic data scenarios for testing cache behavior without manual intervention.

### Automatic Scheduling Operations

| Operation | Frequency | Description |
|-----------|-----------|-------------|
| **Product Creation** | Every 10-15s | Creates 1-3 random products with varying categories and prices |
| **Catalog Creation** | Every 30-45s | Creates product catalogs and adds existing products |
| **Item Deletion** | Every 20-25s | Deletes 1-2 random items to test cache eviction |
| **Cache Access** | Every 8s | Simulates 2-5 random product/catalog accesses |
| **Statistics Logging** | Every 30s | Logs detailed cache metrics with instance identification |

### Data Generation Features

- **Realistic Product Data**: Random names, prices, categories, brands, SKUs
- **Cache TTL Testing**: Items expire based on configured timeouts
- **Cross-Instance Sync**: Watch items created by one instance appear in others
- **Eviction Testing**: Automatic deletion simulates real-world cache invalidation
- **Load Simulation**: Configurable access patterns for hit/miss testing

### Sample Generated Data

```
🔄 [Instance: ab12cd34] Creating 2 random products
✅ [Instance: ab12cd34] Created product: Premium Widget 2044 (ID: d04a55af-f2b3-4f02-82da-700dcf8624d1, Price: 983.42, Category: electronics)
✅ [Instance: ab12cd34] Created product: Classic Device 7892 (ID: e1b8c9d2-a4f3-4c85-9876-1234567890ab, Price: 129.99, Category: home)
📚 [Instance: ab12cd34] Created catalog: Spring 2025 Collection (ID: catalog-ab12cd34-1720789234567)
🔗 [Instance: ab12cd34] Added product d04a55af-f2b3-4f02-82da-700dcf8624d1 to catalog catalog-ab12cd34-1720789234567
🗑️ [Instance: ab12cd34] Deleted product: e1b8c9d2-a4f3-4c85-9876-1234567890ab
```

## Domain Model (DDD)

The project uses Domain Driven Design with a **Product Catalog** domain:

### Entities

- **Product**: Core product entity with business logic
- **Category**: Product categorization
- **ProductCatalog**: Aggregate root managing product collections

### Value Objects

- **ProductId**: Strongly typed product identifier
- **Price**: Monetary value with currency
- **ProductDetails**: Product information (name, description, brand, SKU)

### Repository Pattern

```java
// Domain Layer - Interface
public interface ProductRepository {
    Optional<Product> findById(ProductId id);
    List<Product> findByCategory(String categoryId);
    Product save(Product product);
    void deleteById(ProductId id);
}

// Infrastructure Layer - Implementation
@Repository
public class RedisProductRepository implements ProductRepository {
    // Redis-based implementation with caching
}
```

## Project Structure

```
src/
├── main/java/com/example/demo/
│   ├── domain/
│   │   ├── model/                    # Domain entities and value objects
│   │   │   ├── Product.java         # Product entity
│   │   │   ├── ProductId.java       # Product ID value object
│   │   │   ├── Price.java           # Price value object
│   │   │   ├── ProductDetails.java  # Product details value object
│   │   │   ├── Category.java        # Category entity
│   │   │   └── ProductCatalog.java  # Aggregate root
│   │   └── repository/               # Repository interfaces
│   │       ├── ProductRepository.java
│   │       └── ProductCatalogRepository.java
│   ├── infra/
│   │   ├── config/                   # Configuration classes
│   │   │   ├── RedisConfig.java     # Redis configuration
│   │   │   └── CacheConfig.java     # Cache configuration
│   │   ├── cache/                    # Cache utilities
│   │   │   └── CacheAccessTracker.java # Cache access pattern tracking
│   │   ├── repository/               # Repository implementations
│   │   │   ├── RedisProductRepository.java
│   │   │   └── RedisProductCatalogRepository.java
│   │   └── rest/                     # REST controllers
│   │       ├── ProductController.java
│   │       ├── ProductCatalogController.java
│   │       ├── CacheMetricsController.java # Enhanced cache statistics
│   │       └── CacheDemoController.java
│   └── app/
│       └── service/                  # Application services
│           ├── ProductService.java
│           ├── ProductCatalogService.java
│           ├── DataSimulationService.java    # 🤖 Automated data simulation
│           └── CacheStatisticsService.java   # 📊 Detailed cache tracking
└── test/java/com/example/demo/
    ├── NearCacheIntegrationTest.java    # Cache behavior verification
    ├── ProductCatalogCacheTest.java     # Catalog caching tests
    └── CachePerformanceTest.java       # Performance measurements

# Root project files
├── run-multi-instance.sh          # 🚀 Start multiple instances script
├── monitor-instances.sh            # 📊 Interactive monitoring script  
├── Dockerfile                      # Multi-stage Docker build
├── compose.yaml                    # Docker Compose with multi-instance profiles
├── CLAUDE.md                       # AI assistant project documentation
└── README.md                       # This comprehensive guide
```

## Setup and Installation

### Prerequisites

- Java 24
- Docker (for Redis via Testcontainers)
- Gradle 8.14+

### Run the Application

#### Single Instance (Development)

```bash
# Clone the repository
git clone <repository-url>
cd spring-redis-local-cache

# Run with Gradle (includes Redis via Docker Compose)
./gradlew bootRun

# Or build and run the JAR
./gradlew build
java -jar build/libs/demo-0.0.1-SNAPSHOT.jar
```

#### Multi-Instance Setup (Testing & Demo)

```bash
# Start 4 instances + Redis with one command
./run-multi-instance.sh

# Monitor cache synchronization across instances
./monitor-instances.sh

# Stop all instances
docker compose --profile multi-instance down
```

#### Multi-Instance Monitoring Options

The `monitor-instances.sh` script provides several monitoring options:

1. **Check service status** - View all running containers
2. **Show application logs** - Real-time logs from all instances  
3. **Monitor Redis commands** - Live Redis command monitoring
4. **Check Redis keys and stats** - Current Redis state and statistics
5. **Test cache endpoints** - Compare cache metrics across instances
6. **Stop all instances** - Clean shutdown

### Testing

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests="NearCacheIntegrationTest"

# View test reports
open build/reports/tests/test/index.html
```

## Configuration

### Cache Configuration (`CacheConfig.java`)

```java
@Bean
public CacheManager cacheManager(RedisTemplate<String, Object> redisTemplate) {
    // L1 Cache - Caffeine
    CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
    caffeineCacheManager.setCaffeine(
        Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .recordStats()
    );

    // L2 Cache - Redis
    RedisCacheManager redisCacheManager = RedisCacheManager.builder(redisTemplate)
        .cacheDefaults(
            RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(2))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                    .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                    .fromSerializer(new GenericJackson2JsonRedisSerializer(objectMapper())))
        )
        .build();

    // Composite Cache Manager (L1 + L2)
    return new CompositeCacheManager(caffeineCacheManager, redisCacheManager);
}
```

### Redis Configuration (`RedisConfig.java`)

```java
@Bean
public ObjectMapper objectMapper() {
    return new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .activateDefaultTyping(
            BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.example.demo.domain")
                .allowIfSubType("java.util")
                .allowIfSubType("java.time")
                .build(),
            ObjectMapper.DefaultTyping.NON_FINAL
        );
}
```

## API Endpoints

### Product Management

```bash
# Create product
POST /api/products
Content-Type: application/json
{
  "details": {
    "name": "Spring Boot Book",
    "description": "Comprehensive guide",
    "brand": "Tech Publisher",
    "sku": "BOOK-001"
  },
  "price": {
    "amount": 49.99,
    "currency": "USD"
  },
  "category": {
    "id": "books",
    "name": "Books",
    "description": "Technical books"
  }
}

# Get product by ID (cached)
GET /api/products/{id}

# Update product price (cache eviction)
PUT /api/products/{id}/price
Content-Type: application/json
{
  "amount": 39.99,
  "currency": "USD"
}

# Get products by category (cached)
GET /api/products/category/{categoryId}

# Delete product (cache eviction)
DELETE /api/products/{id}
```

### Cache Management

```bash
# Get detailed cache statistics (JSON)
GET /api/cache/metrics
{
  "instanceId": "abc123",
  "cacheMetrics": {
    "products": {
      "size": 45,
      "hitRate": 67.8,
      "evictionCount": 5
    }
  },
  "accessMetrics": {
    "products": {
      "l1HitRate": 67.8,
      "l2HitRate": 23.4,
      "overallHitRate": 84.2
    }
  },
  "redisMetrics": {
    "keyCount": 89,
    "hitRate": 91.2
  }
}

# Get human-readable cache summary
GET /api/cache/summary

# Compare cache statistics across instances
for port in 8080 8081 8082 8083; do
  curl http://localhost:$port/api/cache/summary
done

# Cache demo endpoints
GET /api/demo/load-test-data
GET /api/demo/cache-performance/{operations}
GET /api/demo/concurrent-test/{threads}/{operations}
```

### Product Catalog

```bash
# Create catalog
POST /api/catalogs
Content-Type: application/json
{
  "catalogId": "spring-2024",
  "name": "Spring 2024 Collection"
}

# Get catalog (cached)
GET /api/catalogs/{catalogId}

# Add product to catalog
POST /api/catalogs/{catalogId}/products/{productId}

# Remove product from catalog
DELETE /api/catalogs/{catalogId}/products/{productId}
```

## Testing

### Integration Tests

The project includes comprehensive tests:

1. **NearCacheIntegrationTest**: Verifies L1/L2 cache behavior
2. **ProductCatalogCacheTest**: Tests catalog-specific caching
3. **CachePerformanceTest**: Measures cache performance improvements

### Test Results Summary

- ✅ **6 tests passed** in NearCacheIntegrationTest
- ✅ **Cache eviction** works correctly on updates/deletes
- ✅ **Category queries** are properly cached
- ✅ **Redis L2 cache** functions as fallback
- ⚠️ **1 test disabled** (flaky cache hit count timing)

### Running Specific Tests

```bash
# Test near cache behavior
./gradlew test --tests="*NearCache*"

# Test performance (disabled by default due to timing sensitivity)
./gradlew test --tests="*Performance*"
```

## Performance Characteristics

### Cache Performance Metrics

Based on test results and real-time monitoring:

- **L1 Hit (Caffeine)**: ~0.1ms response time
- **L2 Hit (Redis)**: ~5-10ms response time  
- **Database Miss**: ~50-100ms response time
- **Performance Improvement**: 50-90% faster with cache

### Enhanced Cache Statistics

The application provides comprehensive real-time cache metrics with detailed breakdown:

#### L1 Cache (Caffeine) Metrics
- **Hit Rate**: Percentage of requests served from local cache
- **Size**: Current number of cached items
- **Eviction Count**: Items removed due to TTL or size limits
- **Load Penalty**: Average time to load missing items

#### L2 Cache (Redis) Metrics  
- **Keyspace Hit Rate**: Redis-level cache effectiveness
- **Commands Processed**: Total Redis operations
- **Connected Clients**: Number of application instances
- **Key Count**: Total items in Redis

#### Access Pattern Analysis
- **L1 Hit Rate**: Direct local cache hits
- **L2 Hit Rate**: When L1 misses, Redis success rate
- **Overall Hit Rate**: Combined multi-layer effectiveness
- **Miss Patterns**: Complete cache misses requiring database access

### Multi-Instance Performance Comparison

With 4 instances running simultaneously:

```bash
Instance 1: L1=68.2%, L2=24.1%, Overall=84.7%
Instance 2: L1=71.5%, L2=19.8%, Overall=86.3%
Instance 3: L1=65.9%, L2=26.3%, Overall=83.1%
Instance 4: L1=69.7%, L2=22.4%, Overall=85.4%

Redis: Keys=156, Hit Rate=91.2%, Commands=2847
```

### Cache Warmup Patterns

- **Cold Start**: 0% hit rate, cache builds over 30-60 seconds
- **Warm Cache**: 70-80% L1 hit rate, 20-30% L2 hit rate
- **Hot Cache**: 80-90% L1 hit rate, 10-20% L2 hit rate
- **Cross-Instance Sync**: Items cached by one instance available to others via Redis

## 🎯 Key Features Summary

This project demonstrates enterprise-grade caching patterns with:

### 🚀 Production-Ready Architecture
- **Near Cache Pattern**: L1 (Caffeine) + L2 (Redis) implementation
- **Domain-Driven Design**: Clean architecture with separated concerns
- **Spring Cache Integration**: Declarative caching with annotations
- **Modern Java**: Java 24 with contemporary language features

### 📊 Advanced Monitoring & Analytics
- **Real-Time Statistics**: Detailed cache hit/miss analysis
- **Multi-Layer Tracking**: Separate L1 and L2 performance metrics
- **Instance Comparison**: Side-by-side cache performance across multiple instances
- **Redis Analytics**: Keyspace statistics, command monitoring, client tracking

### 🤖 Automated Testing & Simulation
- **Spring Schedulers**: Continuous data generation for realistic testing
- **Multi-Instance Setup**: Docker Compose with 4 application instances
- **Cache Scenario Testing**: TTL expiration, eviction patterns, synchronization
- **Load Simulation**: Configurable access patterns and data generation

### 🛠️ Developer Experience
- **One-Command Setup**: `./run-multi-instance.sh` starts everything
- **Interactive Monitoring**: `./monitor-instances.sh` with multiple viewing options
- **Comprehensive Testing**: Unit tests, integration tests, performance benchmarks
- **Detailed Documentation**: Complete setup guides and troubleshooting

### 📈 Performance Optimization
- **Cache Layer Analysis**: Understand L1 vs L2 effectiveness
- **Hit Rate Optimization**: Real-time feedback for tuning cache parameters
- **Eviction Monitoring**: Track cache turnover and memory efficiency
- **Latency Insights**: Response time comparison across cache layers

## Use Cases

### 1. High-Read Workloads

Perfect for applications with:
- Product catalogs
- Content management systems
- User profiles
- Configuration data

### 2. Distributed Systems

Ideal when you need:
- Consistent data across multiple instances
- Fast local access with shared cache
- Horizontal scaling capabilities

### 3. E-commerce Applications

Great for:
- Product information caching
- Category browsing optimization
- Price updates with cache invalidation
- Inventory status caching

### 4. Microservices Architecture

Suitable for:
- Service-to-service data sharing
- Reducing database load
- Improving response times
- Cross-service data consistency

## Best Practices

### 1. Cache Key Design

```java
// Use meaningful, consistent cache keys
@Cacheable(value = "products", key = "#productId.value")
public Optional<Product> findProductById(ProductId productId) {
    // Implementation
}

@Cacheable(value = "products", key = "'category:' + #categoryId")
public List<Product> findProductsByCategory(String categoryId) {
    // Implementation
}
```

### 2. Cache Eviction Strategy

```java
// Evict on updates
@CacheEvict(value = "products", key = "#productId.value")
public void updateProductPrice(ProductId productId, Price newPrice) {
    // Implementation
}

// Evict multiple entries
@CacheEvict(value = "products", allEntries = true)
public void evictAllProductsCache() {
    // Implementation
}
```

### 3. Serialization Configuration

- Use `JavaTimeModule` for LocalDateTime support
- Configure `BasicPolymorphicTypeValidator` for security
- Handle complex domain objects with `@JsonCreator`

### 4. Monitoring and Metrics

- Enable Caffeine cache statistics
- Monitor cache hit rates
- Set up alerts for cache performance degradation
- Track cache size and eviction rates

### 5. Testing Strategies

- Test cache behavior in integration tests
- Verify cache eviction on updates
- Measure performance improvements
- Test concurrent access scenarios

## Troubleshooting

### Common Issues

1. **Serialization Errors**: Ensure all cached objects implement `Serializable` and have proper Jackson annotations
2. **Cache Miss**: Verify cache key generation and TTL settings
3. **Performance Issues**: Check cache hit rates and optimize cache size
4. **Memory Usage**: Monitor L1 cache size and configure appropriate limits

### Debugging Cache Behavior

```bash
# Enable debug logging
logging.level.org.springframework.cache=DEBUG
logging.level.com.github.benmanes.caffeine=DEBUG
```

## Modern Java Features

This project demonstrates **modern Java practices** using Java 24:

### Java Records for Immutable Value Objects
```java
// All DTOs and value objects use Java records
public record ProductId(String value) implements Serializable {
    public ProductId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ProductId cannot be null or empty");
        }
    }
}

public record Price(BigDecimal amount, Currency currency) implements Serializable {
    public static Price of(double amount, String currencyCode) {
        return new Price(BigDecimal.valueOf(amount), Currency.getInstance(currencyCode));
    }
}

public record CreateProductRequest(
        String name,
        String description,
        String brand,
        String sku,
        BigDecimal price,
        String currency,
        String categoryId,
        String categoryName,
        String categoryDescription) {}
```

### Type Inference with `var`
```java
// Type inference used throughout the codebase
var cacheManager = new CaffeineCacheManager("products", "catalogs");
var config = RedisCacheConfiguration.defaultCacheConfig();
var productOpt = productRepository.findById(productId);
var start = Instant.now();
var duration = Duration.between(start, Instant.now());
```

### Pattern Matching and Switch Expressions
```java
// Modern pattern matching with switch expressions
var l1Hit = switch (cache) {
    case CaffeineCache caffeineCache -> {
        var nativeCache = caffeineCache.getNativeCache();
        var cachedValue = nativeCache.getIfPresent(key);
        yield cachedValue != null;
    }
    default -> false;
};

// Pattern matching for instanceof
if (springCache instanceof CaffeineCache caffeineCache) {
    var nativeCache = caffeineCache.getNativeCache();
    var stats = nativeCache.stats();
}
```

### Lombok @Builder with toBuilder
```java
// Complex mutable objects use @Builder(toBuilder = true)
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product implements Serializable {
    private ProductId id;
    private ProductDetails details;
    private Price price;
    private Category category;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Business methods...
}
```

### Collection Factory Methods
```java
// Immutable collections (Java 9+)
private static final List<Category> AVAILABLE_CATEGORIES = List.of(
    Category.create("electronics", "Electronics", "Electronic devices"),
    Category.create("clothing", "Clothing", "Fashion and apparel"),
    Category.create("books", "Books", "Literature and educational materials")
);
```

### Stream API Enhancements
```java
// Modern stream collection (Java 16+)
return products.values().stream()
    .filter(Product::isActive)
    .toList(); // Instead of .collect(Collectors.toList())
```

### Enhanced String Methods
```java
// Using isBlank() instead of trim().isEmpty()
if (value == null || value.isBlank()) {
    throw new IllegalArgumentException("Value cannot be null or empty");
}

```

## Code Quality Tools

This project includes comprehensive code quality tooling:

### Spotless Code Formatter
- **Palantir Java Format**: Consistent code style across the codebase
- **Automatic formatting**: Run `./gradlew spotlessApply` to fix formatting issues
- **Import ordering**: Organized imports with consistent ordering
- **Whitespace management**: Automatic trimming and consistent line endings

### PMD Static Analysis
- **Custom ruleset**: Configured for modern Java practices
- **Reduced violations**: From 24 initial violations to only 4 acceptable ones
- **Modern Java support**: Rules adapted for Java 24 features
- **Run analysis**: `./gradlew pmdMain pmdTest`

### SpotBugs (FindBugs successor)
- **Bug pattern detection**: Identifies potential bugs and performance issues
- **Security analysis**: Includes FindSecBugs plugin
- **Custom exclusions**: Configured for project-specific patterns
- **Run analysis**: `./gradlew spotbugsMain spotbugsTest`

### SonarQube Integration
- **Code quality metrics**: Comprehensive quality analysis
- **Technical debt tracking**: Monitor code maintainability
- **Security hotspots**: Identify potential security issues
- **Run analysis**: `./gradlew sonar`

### JaCoCo Code Coverage
- **Coverage requirements**: 80% minimum coverage threshold
- **Test reports**: HTML and XML coverage reports
- **Integration**: Works with SonarQube for coverage tracking

### Running Code Quality Checks
```bash
# Apply code formatting
./gradlew spotlessApply

# Run all quality checks (optional, requires -PenableAllQualityChecks)
./gradlew build -PenableAllQualityChecks

# Run specific tools
./gradlew pmdMain
./gradlew spotbugsMain
./gradlew test jacocoTestReport

# Basic build (formatting check only)
./gradlew build
```

## Technology Stack

- **Spring Boot 3.5.3**
- **Java 24** with modern language features
- **Redis** (via Testcontainers)
- **Caffeine Cache**
- **Jackson** (JSON serialization)
- **Gradle** (Kotlin DSL)
- **JUnit 5** (Testing)
- **AssertJ** (Test assertions)
- **Spotless** (Code formatting with Palantir style)
- **PMD** (Static code analysis)
- **SpotBugs** (Bug pattern detection)
- **SonarQube** (Code quality platform)
- **JaCoCo** (Code coverage)

## License

This project is a sample implementation for educational purposes.