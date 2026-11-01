package com.twchrome.binance_price_service.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
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
    private volatile Instant pausedUntil = Instant.EPOCH;

    private static final Logger log = LoggerFactory.getLogger(PriceCollectorService.class);

    private final Clock clock;
    static final int DEFAULT_PAUSE_SECONDS = 90;

    public PriceCollectorService(BinanceClient client, PriceRepository repository, BinanceProperties properties, Clock clock) {
        this.client = client;
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
    }

    private Instant calculatePausedUntil(HttpHeaders headers) {
        if (headers == null) {
            return Instant.now(clock).plusSeconds(DEFAULT_PAUSE_SECONDS);
        }

        String retryAfter = headers.getFirst(HttpHeaders.RETRY_AFTER);

        Instant pausedUntilTmp;

        if (retryAfter != null) {
            if (retryAfter.matches("^\\d+$")) {
                
                try {
                    long seconds = Long.parseLong(retryAfter);
                    pausedUntilTmp = Instant.now(clock).plusSeconds(seconds);
                } catch (NumberFormatException e) {
                    log.warn("Incorected format of data: {} | Use default pause: {}", e.getMessage(), DEFAULT_PAUSE_SECONDS);
                    pausedUntilTmp = Instant.now(clock).plusSeconds(DEFAULT_PAUSE_SECONDS);
                }
            } else {
                try {
                    Instant now = Instant.now(clock);
                    Instant requestedPausedUntil = ZonedDateTime.parse(retryAfter, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();

                    if (!requestedPausedUntil.isAfter(now)) {
                        pausedUntilTmp = now.plusSeconds(DEFAULT_PAUSE_SECONDS);
                    } else {
                        pausedUntilTmp = requestedPausedUntil;
                    }
                } catch (DateTimeParseException e) {
                    pausedUntilTmp = Instant.now(clock).plusSeconds(DEFAULT_PAUSE_SECONDS);
                }
            }
        } else {
            pausedUntilTmp = Instant.now(clock).plusSeconds(DEFAULT_PAUSE_SECONDS);
        }

        return pausedUntilTmp;
    }

    @Scheduled(fixedDelayString = "${binance.poll-interval}")
    public void getAndSave() {
        if (Instant.now(clock).isBefore(pausedUntil)) {
            log.info("Pause until: {}", pausedUntil);
            return;
        }

        String symbol = properties.symbol();

        AvgPriceResponse response;

        try {
            response = client.fetchAvgPrice(symbol);
        } catch (HttpClientErrorException e) {
            int statusCode = e.getStatusCode().value();
            HttpHeaders headers = e.getResponseHeaders();

            if (statusCode == 429 || statusCode == 418) {
                pausedUntil = calculatePausedUntil(headers);
            }

            log.warn("Error 4xx: {} | Answer: {}", statusCode, e.getResponseBodyAsString());
            return;
        } catch (HttpServerErrorException e) {
            log.warn("Server's error 5xx: {}", e.getStatusCode());
            return;
        } catch (ResourceAccessException e) {
            log.warn("Binance is unreachable or timed out: {}", e.getMessage());
            return;
        } catch (RestClientException e) {
            log.warn("Unknown error: {}", e.getMessage());
            return;
        }

        if (repository.existsBySymbolAndCloseTime(symbol, response.closeTimeAtInstant())) {
            return;
        }

        Price price = new Price();
        price.setSymbol(symbol);
        price.setCloseTime(response.closeTimeAtInstant());
        price.setPrice(response.price());
        price.setFetchedAt(Instant.now(clock));

        try {
            repository.save(price);
        } catch (DataIntegrityViolationException e) {
            log.debug("This line is exist {}, {}", symbol, response.closeTime());
        } catch (DataAccessException e) {
            log.warn("Failed to save price for {} at {}", symbol, response.closeTime(), e);
        }
    }

}
