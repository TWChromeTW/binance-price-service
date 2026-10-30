package com.twchrome.binance_price_service.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "binance") 
public record BinanceProperties(
    String symbol,
    String baseUrl,
    Duration pollInterval
) {

}
