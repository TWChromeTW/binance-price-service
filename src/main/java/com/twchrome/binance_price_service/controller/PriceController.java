package com.twchrome.binance_price_service.controller;

import java.time.Instant;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.twchrome.binance_price_service.config.BinanceProperties;
import com.twchrome.binance_price_service.dto.AveragePriceResponse;
import com.twchrome.binance_price_service.dto.PriceResponse;
import com.twchrome.binance_price_service.service.PriceQueryService;



@RestController
@RequestMapping("/api/v1/prices")
public class PriceController {

    private final PriceQueryService priceQueryService;
    private final BinanceProperties properties;

    public PriceController(PriceQueryService priceQueryService, BinanceProperties properties) {
        this.priceQueryService = priceQueryService;
        this.properties = properties;
    }

    @GetMapping("/latest")
    public ResponseEntity<PriceResponse> getLatestPrice() {
        return priceQueryService.findLatest()
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
    
    @GetMapping("/average")
    public ResponseEntity<AveragePriceResponse> getAvgPriceBetween(
        @RequestParam("from") Instant from,
        @RequestParam("to") Instant to
    ) {
        if (to.isBefore(from)) {
            return ResponseEntity.badRequest().build();
        }

        return priceQueryService.avgPriceBetween(from, to, properties.symbol())
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
    

}
