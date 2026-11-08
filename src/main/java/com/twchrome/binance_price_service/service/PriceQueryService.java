package com.twchrome.binance_price_service.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import com.twchrome.binance_price_service.config.BinanceProperties;
import com.twchrome.binance_price_service.dto.AveragePriceResponse;
import com.twchrome.binance_price_service.dto.PageablePriceResponse;
import com.twchrome.binance_price_service.dto.PriceResponse;
import com.twchrome.binance_price_service.dto.Trend;
import com.twchrome.binance_price_service.dto.TrendResponse;
import com.twchrome.binance_price_service.entity.Price;
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
                price.getFetchedAt().truncatedTo(ChronoUnit.SECONDS)
            ));
    }

    public Optional<AveragePriceResponse> avgPriceBetween(Instant from, Instant to) {
        return repository.getAveragePriceBetween(from, to, properties.symbol())
            .map(price -> new AveragePriceResponse(price.setScale(8, RoundingMode.HALF_UP)));
    }

    public PageablePriceResponse pageableRecords(Instant from, Instant to, Pageable pageable) {
        Page<PriceResponse> responsePage = repository
            .findAllBySymbolAndFetchedAtBetween(properties.symbol(), from, to, pageable)
            .map(price -> new PriceResponse(
                price.getSymbol(),
                price.getPrice(),
                price.getFetchedAt().truncatedTo(ChronoUnit.SECONDS)
            ));

        return new PageablePriceResponse(
            responsePage.getContent(),
            responsePage.getNumber(),
            responsePage.getSize(),
            responsePage.getTotalElements(),
            responsePage.hasNext()
        );
    }

    public Optional<TrendResponse> getTrendNotes(Instant from, Instant to) {
        Optional<Price> firstPrice = repository
            .findFirstBySymbolAndFetchedAtBetweenOrderByFetchedAtAscIdAsc(
                properties.symbol(), 
                from, 
                to
            );
        Optional<Price> lastPrice = repository
            .findFirstBySymbolAndFetchedAtBetweenOrderByFetchedAtDescIdDesc(
                properties.symbol(), 
                from, 
                to
            );

        if (firstPrice.isEmpty() || lastPrice.isEmpty()) {
            return Optional.empty();
        }
        
        if (firstPrice.get().getId().equals(lastPrice.get().getId())) {
            return Optional.empty();
        }
    
        BigDecimal first = firstPrice.get().getPrice();
        BigDecimal last = lastPrice.get().getPrice();

        Trend trend;
        int comparison = last.compareTo(first);

        if (comparison > 0) {
            trend = Trend.UPWARD;
        } else if (comparison < 0) {
            trend = Trend.DOWNWARD;
        } else {
            trend = Trend.FLAT;
        }

        return Optional.of(new TrendResponse(
            properties.symbol(),
            from,
            to,
            trend
        ));
    
    }
}
