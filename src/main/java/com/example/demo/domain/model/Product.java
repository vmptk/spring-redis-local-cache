package com.example.demo.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product implements Serializable {
    private ProductId id;
    private ProductDetails details;
    private Price price;
    private Category category;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @JsonCreator
    public Product(@JsonProperty("id") ProductId id,
                  @JsonProperty("details") ProductDetails details,
                  @JsonProperty("price") Price price,
                  @JsonProperty("category") Category category,
                  @JsonProperty("active") boolean active,
                  @JsonProperty("createdAt") LocalDateTime createdAt,
                  @JsonProperty("updatedAt") LocalDateTime updatedAt) {
        this.id = id;
        this.details = details;
        this.price = price;
        this.category = category;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Product create(ProductDetails details, Price price, Category category) {
        return new Product(
                ProductId.generate(),
                details,
                price,
                category,
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    public void updatePrice(Price newPrice) {
        if (newPrice == null) {
            throw new IllegalArgumentException("Price cannot be null");
        }
        this.price = newPrice;
        this.updatedAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = LocalDateTime.now();
    }

    public void activate() {
        this.active = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateDetails(ProductDetails newDetails) {
        if (newDetails == null) {
            throw new IllegalArgumentException("Product details cannot be null");
        }
        this.details = newDetails;
        this.updatedAt = LocalDateTime.now();
    }
}