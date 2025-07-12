package com.example.demo.app.controller.dto;

import java.math.BigDecimal;

public record UpdatePriceRequest(BigDecimal price, String currency) {}
