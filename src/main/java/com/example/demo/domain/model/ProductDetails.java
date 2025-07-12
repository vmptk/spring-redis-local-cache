package com.example.demo.domain.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record ProductDetails(String name, String description, String brand, String sku) implements Serializable {

    @JsonCreator
    public ProductDetails(
            @JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("brand") String brand,
            @JsonProperty("sku") String sku) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Product name cannot be null or empty");
        }
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("Product SKU cannot be null or empty");
        }
        this.name = name;
        this.description = description;
        this.brand = brand;
        this.sku = sku;
    }

    public static ProductDetails create(String name, String description, String brand, String sku) {
        return new ProductDetails(name, description, brand, sku);
    }
}
