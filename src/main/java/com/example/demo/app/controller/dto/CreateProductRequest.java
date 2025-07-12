package com.example.demo.app.controller.dto;

import java.math.BigDecimal;

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
