package com.example.demo.app.controller.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class UpdatePriceRequest {
    private BigDecimal price;
    private String currency;
}