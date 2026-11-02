package com.twchrome.binance_price_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.twchrome.binance_price_service.dto.PriceResponse;
import com.twchrome.binance_price_service.service.PriceQueryService;


@RestController
@RequestMapping("/api/v1/prices")
public class PriceController {

    private final PriceQueryService priceQueryService;

    public PriceController(PriceQueryService priceQueryService) {
        this.priceQueryService = priceQueryService;
    }

    @GetMapping("/latest")
    public ResponseEntity<PriceResponse> getLatestPrice() {
        return priceQueryService.findLatest()
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
    

}
