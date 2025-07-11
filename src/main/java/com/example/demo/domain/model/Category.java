package com.example.demo.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category implements Serializable {
    private String id;
    private String name;
    private String description;

    @JsonCreator
    public Category(@JsonProperty("id") String id,
                   @JsonProperty("name") String name,
                   @JsonProperty("description") String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public static Category create(String id, String name, String description) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Category ID cannot be null or empty");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Category name cannot be null or empty");
        }
        return new Category(id, name, description);
    }
}