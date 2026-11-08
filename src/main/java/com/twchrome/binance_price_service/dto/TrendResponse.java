package com.twchrome.binance_price_service.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

public record TrendResponse(
    String symbol,
    @JsonProperty("start_date")
    @JsonFormat(
        shape = JsonFormat.Shape.STRING,
        pattern = "yyyy-MM-dd",
        timezone = "UTC"
    )
    Instant startDate,
    @JsonProperty("end_date")
    @JsonFormat(
        shape = JsonFormat.Shape.STRING,
        pattern = "yyyy-MM-dd",
        timezone = "UTC"
    )
    Instant endDate,
    Trend trend
) {

}
