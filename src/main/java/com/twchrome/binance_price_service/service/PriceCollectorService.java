package com.twchrome.binance_price_service.service;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import com.twchrome.binance_price_service.client.AvgPriceResponse;
import com.twchrome.binance_price_service.client.BinanceClient;
import com.twchrome.binance_price_service.config.BinanceProperties;
import com.twchrome.binance_price_service.entity.Price;
import com.twchrome.binance_price_service.repository.PriceRepository;

@Service
public class PriceCollectorService {

    private final BinanceClient client;
    private final PriceRepository repository;
    private final BinanceProperties properties;

    private static final Logger log = LoggerFactory.getLogger(PriceCollectorService.class);

    public PriceCollectorService(BinanceClient client, PriceRepository repository, BinanceProperties properties) {
        this.client = client;
        this.repository = repository;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${binance.poll-interval}")
    public void getAndSave() {
        String symbol = properties.symbol();

        AvgPriceResponse response;

        try {
            response = client.fetchAvgPrice(symbol);
        } catch (HttpClientErrorException e) {
            log.warn("Error 4xx: " + e.getStatusCode() + " | Answer: " + e.getResponseBodyAsString());
            return;
        } catch (HttpServerErrorException e) {
            log.warn("Server's error 5xx: " + e.getStatusCode());
            return;
        } catch (ResourceAccessException e) {
            log.warn("Binance is unreachable or timed out: " + e.getMessage());
            return;
        } catch (RestClientException e) {
            log.warn("Unknown error: " + e.getMessage());
            return;
        }

        if (repository.existsBySymbolAndCloseTime(symbol, response.closeTimeAtInstant())){
            return;
        }

        Price price = new Price();
        price.setSymbol(symbol);
        price.setCloseTime(response.closeTimeAtInstant());
        price.setPrice(response.price());
        price.setFetchedAt(Instant.now());

        repository.save(price);
    }

}
