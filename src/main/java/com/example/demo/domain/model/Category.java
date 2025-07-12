package com.example.demo.domain.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record Category(String id, String name, String description) implements Serializable {

    @JsonCreator
    public Category(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("description") String description) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Category ID cannot be null or empty");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Category name cannot be null or empty");
        }
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public static Category create(String id, String name, String description) {
        return new Category(id, name, description);
    }
}
