package com.example.demo.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Currency;

@Value
public class Price implements Serializable {
    BigDecimal amount;
    Currency currency;

    @JsonCreator
    public Price(@JsonProperty("amount") BigDecimal amount, 
                 @JsonProperty("currency") Currency currency) {
        this.amount = amount;
        this.currency = currency;
    }

    public static Price of(BigDecimal amount, String currencyCode) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Price amount must be non-negative");
        }
        return new Price(amount, Currency.getInstance(currencyCode));
    }

    public static Price of(double amount, String currencyCode) {
        return of(BigDecimal.valueOf(amount), currencyCode);
    }

    public String format() {
        return currency.getSymbol() + amount.setScale(2, java.math.RoundingMode.HALF_UP);
    }
}