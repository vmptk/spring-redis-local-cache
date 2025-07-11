package com.example.demo.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;
import java.io.Serializable;
import java.util.UUID;

@Value
public class ProductId implements Serializable {
    String value;

    @JsonCreator
    public ProductId(@JsonProperty("value") String value) {
        this.value = value;
    }

    public static ProductId generate() {
        return new ProductId(UUID.randomUUID().toString());
    }

    public static ProductId of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("ProductId cannot be null or empty");
        }
        return new ProductId(value);
    }
}