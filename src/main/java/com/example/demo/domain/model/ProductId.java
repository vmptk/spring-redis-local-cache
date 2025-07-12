package com.example.demo.domain.model;

import java.io.Serializable;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record ProductId(String value) implements Serializable {

    @JsonCreator
    public ProductId(@JsonProperty("value") String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ProductId cannot be null or empty");
        }
        this.value = value;
    }

    public static ProductId generate() {
        return new ProductId(UUID.randomUUID().toString());
    }

    public static ProductId of(String value) {
        return new ProductId(value);
    }
}
