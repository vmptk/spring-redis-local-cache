package com.example.demo.app.controller.dto;

import lombok.Data;

@Data
public class CreateCatalogRequest {
    private String catalogId;
    private String name;
}