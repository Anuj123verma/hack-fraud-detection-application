package com.meridiantrust.sentinel.service;

import com.meridiantrust.sentinel.config.AmlProperties;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Normalizes any transaction amount to the configured base currency (INR by
 * default) using a simple, config-driven rate table - satisfies "all
 * monetary amounts must be normalized... using a configurable exchange rate
 * table" without needing a full admin CRUD API in the hackathon timebox.
 */
@Service
public class CurrencyService {

    private final AmlProperties properties;

    public CurrencyService(AmlProperties properties) {
        this.properties = properties;
    }

    public BigDecimal toBase(BigDecimal amount, String currencyCode) {
        Double rate = properties.getCurrency().getRates().get(currencyCode);
        if (rate == null) {
            throw new IllegalArgumentException("No exchange rate configured for currency: " + currencyCode);
        }
        return amount.multiply(BigDecimal.valueOf(rate)).setScale(2, RoundingMode.HALF_UP);
    }

    public String baseCurrency() {
        return properties.getCurrency().getBase();
    }
}
