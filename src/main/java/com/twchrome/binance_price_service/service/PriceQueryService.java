package com.twchrome.binance_price_service.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.twchrome.binance_price_service.config.BinanceProperties;
import com.twchrome.binance_price_service.dto.PriceResponse;
import com.twchrome.binance_price_service.repository.PriceRepository;

@Service 
public class PriceQueryService {

    private final PriceRepository repository;
    private final BinanceProperties properties;

    public PriceQueryService(PriceRepository repository, BinanceProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    public Optional<PriceResponse> findLatest() {
        return repository.findFirstBySymbolOrderByFetchedAtDesc(properties.symbol())
            .map(price -> new PriceResponse(
                price.getSymbol(),
                price.getPrice(),
                price.getFetchedAt()
            ));
    }
}
