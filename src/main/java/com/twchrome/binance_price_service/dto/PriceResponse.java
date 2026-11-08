package com.twchrome.binance_price_service.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonFormat;

public record PriceResponse(
    String symbol,
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    BigDecimal price,
    Instant fetchedAt
) {

}
