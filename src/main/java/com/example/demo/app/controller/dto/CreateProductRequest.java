package com.example.demo.app.controller.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CreateProductRequest {
    private String name;
    private String description;
    private String brand;
    private String sku;
    private BigDecimal price;
    private String currency;
    private String categoryId;
    private String categoryName;
    private String categoryDescription;
}