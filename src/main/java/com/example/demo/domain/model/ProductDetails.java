package com.example.demo.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;
import java.io.Serializable;

@Value
public class ProductDetails implements Serializable {
    String name;
    String description;
    String brand;
    String sku;

    @JsonCreator
    public ProductDetails(@JsonProperty("name") String name,
                         @JsonProperty("description") String description,
                         @JsonProperty("brand") String brand,
                         @JsonProperty("sku") String sku) {
        this.name = name;
        this.description = description;
        this.brand = brand;
        this.sku = sku;
    }

    public static ProductDetails create(String name, String description, String brand, String sku) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Product name cannot be null or empty");
        }
        if (sku == null || sku.trim().isEmpty()) {
            throw new IllegalArgumentException("Product SKU cannot be null or empty");
        }
        return new ProductDetails(name, description, brand, sku);
    }
}