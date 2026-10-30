package com.twchrome.binance_price_service.client;

import java.math.BigDecimal;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AvgPriceResponse(
    BigDecimal price,
    long closeTime
) {
    public Instant closeTimeAtInstant() {
        return Instant.ofEpochMilli(closeTime());
    }
}
