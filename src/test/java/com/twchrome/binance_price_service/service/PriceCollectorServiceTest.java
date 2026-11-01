package com.twchrome.binance_price_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpClientErrorException;

import com.twchrome.binance_price_service.client.AvgPriceResponse;
import com.twchrome.binance_price_service.client.BinanceClient;
import com.twchrome.binance_price_service.config.BinanceProperties;
import com.twchrome.binance_price_service.entity.Price;
import com.twchrome.binance_price_service.repository.PriceRepository;

@ExtendWith(MockitoExtension.class)
class PriceCollectorServiceTest {

    private static class MutableClock extends Clock {

        private final AtomicReference<Instant> currentInstant;
        private final ZoneId zone;

        MutableClock(Instant initialInstant) {
            this(new AtomicReference<>(initialInstant), ZoneOffset.UTC);
        }

        private MutableClock(AtomicReference<Instant> currentInstant, ZoneId zone) {
            this.currentInstant = currentInstant;
            this.zone = zone;
        }

        void setInstant(Instant instant) {
            currentInstant.set(instant);
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Instant instant() {
            return currentInstant.get();
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(currentInstant, zone);
        }

    }

    @Mock BinanceClient client;
    @Mock PriceRepository repository;

    PriceCollectorService service;

    private MutableClock clock;

    @BeforeEach 
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));

        BinanceProperties properties = new BinanceProperties("BNBUSDT", "http://localhost", Duration.ofSeconds(30));
        service = new PriceCollectorService(client, repository, properties, clock);
    }

    @Test
    void pausesPollingAfterTooManyRequests() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.RETRY_AFTER, "120");
        when(client.fetchAvgPrice("BNBUSDT")).thenThrow(
            HttpClientErrorException.create(HttpStatusCode.valueOf(429), "Too Many Requests", headers, new byte[0], null)
        );

        Instant start = clock.instant();

        service.getAndSave();
        clock.setInstant(start.plusSeconds(119));
        service.getAndSave();
        verify(client, times(1)).fetchAvgPrice("BNBUSDT");

        clock.setInstant(start.plusSeconds(121));
        service.getAndSave();
        verify(client, times(2)).fetchAvgPrice("BNBUSDT");
    }

    @Test
    void pausesWithDefaultDelayWhenRetryAfterMissing(){
        HttpHeaders headers = new HttpHeaders();
        when(client.fetchAvgPrice("BNBUSDT")).thenThrow(
            HttpClientErrorException.create(HttpStatusCode.valueOf(429), "empty", headers, new byte[0], null)
        );

        Instant start = clock.instant();

        service.getAndSave();
        clock.setInstant(start.plusSeconds(PriceCollectorService.DEFAULT_PAUSE_SECONDS - 1));
        service.getAndSave();
        verify(client, times(1)).fetchAvgPrice("BNBUSDT");

        clock.setInstant(start.plusSeconds(PriceCollectorService.DEFAULT_PAUSE_SECONDS + 1));
        service.getAndSave();
        verify(client, times(2)).fetchAvgPrice("BNBUSDT");
    }

    @Test 
    void headersHasDate() {
        ZonedDateTime dateTime = ZonedDateTime.ofInstant(clock.instant().plusSeconds(120), ZoneOffset.UTC);
        String retryAt = dateTime.format(DateTimeFormatter.RFC_1123_DATE_TIME);

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.RETRY_AFTER, retryAt);
        when(client.fetchAvgPrice("BNBUSDT")).thenThrow(
            HttpClientErrorException.create(HttpStatusCode.valueOf(429), "Too Many Requests", headers, new byte[0], null)
        );

        Instant start = clock.instant();

        service.getAndSave();
        clock.setInstant(start.plusSeconds(119));
        service.getAndSave();
        verify(client, times(1)).fetchAvgPrice("BNBUSDT");

        clock.setInstant(start.plusSeconds(121));
        service.getAndSave();
        verify(client, times(2)).fetchAvgPrice("BNBUSDT");
    }

    @Test
    void doesNotSaveWhenPriceAlreadyExists() {
        long closeTimeMillis = 1_700_000_000_000L;
        Instant closeTime = Instant.ofEpochMilli(closeTimeMillis);

        when(client.fetchAvgPrice("BNBUSDT"))
            .thenReturn(new AvgPriceResponse(new BigDecimal("123.45"), closeTimeMillis));
        when(repository.existsBySymbolAndCloseTime("BNBUSDT", closeTime))
            .thenReturn(true);

        service.getAndSave();

        verify(repository).existsBySymbolAndCloseTime("BNBUSDT", closeTime);
        verify(repository, never()).save(any(Price.class));
    }

    @Test
    void savesPriceWhenDoesNotAlreadyExist() {
        long closeTimeMillis = 1_700_000_000_000L;
        Instant closeTime = Instant.ofEpochMilli(closeTimeMillis);
        BigDecimal expectedPrice = new BigDecimal("123.45");

        when(client.fetchAvgPrice("BNBUSDT"))
            .thenReturn(new AvgPriceResponse(expectedPrice, closeTimeMillis));
        when(repository.existsBySymbolAndCloseTime("BNBUSDT", closeTime))
            .thenReturn(false);

        service.getAndSave();

        ArgumentCaptor<Price> priceCaptor = ArgumentCaptor.forClass(Price.class);
        verify(repository).save(priceCaptor.capture());

        Price savedPrice = priceCaptor.getValue();
        assertEquals("BNBUSDT", savedPrice.getSymbol());
        assertEquals(expectedPrice, savedPrice.getPrice());
        assertEquals(closeTime, savedPrice.getCloseTime());
    }

}
