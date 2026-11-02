package com.twchrome.binance_price_service.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PriceResponse(
    String symbol,
    BigDecimal price,
    Instant fetchedAt
) {

}
