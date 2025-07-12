package com.example.demo.domain.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record Price(BigDecimal amount, Currency currency) implements Serializable {

    @JsonCreator
    public Price(@JsonProperty("amount") BigDecimal amount, @JsonProperty("currency") Currency currency) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Price amount must be non-negative");
        }
        this.amount = amount;
        this.currency = currency;
    }

    public static Price of(BigDecimal amount, String currencyCode) {
        return new Price(amount, Currency.getInstance(currencyCode));
    }

    public static Price of(double amount, String currencyCode) {
        return of(BigDecimal.valueOf(amount), currencyCode);
    }

    public String format() {
        return currency.getSymbol() + amount.setScale(2, RoundingMode.HALF_UP);
    }
}
