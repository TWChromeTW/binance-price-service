package com.twchrome.binance_price_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.twchrome.binance_price_service.config.BinanceProperties;
import com.twchrome.binance_price_service.dto.AveragePriceResponse;
import com.twchrome.binance_price_service.dto.PageablePriceResponse;
import com.twchrome.binance_price_service.dto.PriceResponse;
import com.twchrome.binance_price_service.dto.Trend;
import com.twchrome.binance_price_service.dto.TrendResponse;
import com.twchrome.binance_price_service.entity.Price;
import com.twchrome.binance_price_service.repository.PriceRepository;

@ExtendWith(MockitoExtension.class)
public class PriceQueryServiceTest {

    @Mock PriceRepository repository;
    PriceQueryService service;

    @BeforeEach
    void setUp() {
        BinanceProperties properties = new BinanceProperties(
            "BNBUSDT",
            "http://localhost",
            Duration.ofSeconds(30)
        );
        service = new PriceQueryService(repository, properties);
    }

    @Test
    void averagePriceIsRoundedToEightDecimals() {
        Instant from = Instant.parse("2026-11-01T00:00:00Z");
        Instant to = Instant.parse("2026-11-02T00:00:00Z");
        when(repository.getAveragePriceBetween(from, to, "BNBUSDT"))
            .thenReturn(Optional.of(new BigDecimal("702.0545401136666667")));

        Optional<AveragePriceResponse> result = service.avgPriceBetween(from, to);

        assertTrue(result.isPresent());
        assertEquals(0, new BigDecimal("702.05454011").compareTo(result.get().averagePrice()));
    }

    @Test
    void returnsEmptyWhenAverageIsUnavailable() {
        Instant from = Instant.parse("2026-11-01T00:00:00Z");
        Instant to = Instant.parse("2026-11-02T00:00:00Z");
        when(repository.getAveragePriceBetween(from, to, "BNBUSDT"))
            .thenReturn(Optional.empty());

        Optional<AveragePriceResponse> result = service.avgPriceBetween(from, to);

        assertTrue(result.isEmpty());
    }

    private Price price(long id, String value) {
        Price p = new Price();
        p.setId(id);
        p.setSymbol("BNBUSDT");
        p.setPrice(new BigDecimal(value));

        return p;
    }

    @Test
    void returnUpwardWhenLastPriceIsHigher() {
        Price firstPrice = price(0, "701.05125739");
        Price lastPrice = price(1, "702.05125739");

        Instant from = Instant.parse("2026-11-01T00:00:00Z");
        Instant to = Instant.parse("2026-11-02T00:00:00Z");

        when(repository.findFirstBySymbolAndFetchedAtBetweenOrderByFetchedAtAscIdAsc("BNBUSDT", from, to))
            .thenReturn(Optional.of(firstPrice));

        when(repository.findFirstBySymbolAndFetchedAtBetweenOrderByFetchedAtDescIdDesc("BNBUSDT", from, to))
            .thenReturn(Optional.of(lastPrice));

        Optional<TrendResponse> result = service.getTrendNotes(from, to);

        assertTrue(result.isPresent());
        assertEquals(Trend.UPWARD, result.get().trend());
        assertEquals("BNBUSDT", result.get().symbol());
        assertEquals(from, result.get().startDate());
        assertEquals(to, result.get().endDate());
    }

    @Test
    void returnDownwardWhenLastPriceIsLower() {
        Price firstPrice = price(0, "702.05125739");
        Price lastPrice = price(1, "701.05125739");

        Instant from = Instant.parse("2026-11-01T00:00:00Z");
        Instant to = Instant.parse("2026-11-02T00:00:00Z");

        when(repository.findFirstBySymbolAndFetchedAtBetweenOrderByFetchedAtAscIdAsc("BNBUSDT", from, to))
            .thenReturn(Optional.of(firstPrice));

        when(repository.findFirstBySymbolAndFetchedAtBetweenOrderByFetchedAtDescIdDesc("BNBUSDT", from, to))
            .thenReturn(Optional.of(lastPrice));

        Optional<TrendResponse> result = service.getTrendNotes(from, to);

        assertTrue(result.isPresent());
        assertEquals(Trend.DOWNWARD, result.get().trend());
    }

    @Test
    void returnFlatWhenLastPriceIsSame() {
        Price firstPrice = price(0, "701.05125739");
        Price lastPrice = price(1, "701.05125739");

        Instant from = Instant.parse("2026-11-01T00:00:00Z");
        Instant to = Instant.parse("2026-11-02T00:00:00Z");

        when(repository.findFirstBySymbolAndFetchedAtBetweenOrderByFetchedAtAscIdAsc("BNBUSDT", from, to))
            .thenReturn(Optional.of(firstPrice));

        when(repository.findFirstBySymbolAndFetchedAtBetweenOrderByFetchedAtDescIdDesc("BNBUSDT", from, to))
            .thenReturn(Optional.of(lastPrice));

        Optional<TrendResponse> result = service.getTrendNotes(from, to);

        assertTrue(result.isPresent());
        assertEquals(Trend.FLAT, result.get().trend());
    }

    @Test
    void returnEmptyWherFirstAndLastAreEmpty() {
        Instant from = Instant.parse("2026-11-01T00:00:00Z");
        Instant to = Instant.parse("2026-11-02T00:00:00Z");

        when(repository.findFirstBySymbolAndFetchedAtBetweenOrderByFetchedAtAscIdAsc("BNBUSDT", from, to))
            .thenReturn(Optional.empty());

        when(repository.findFirstBySymbolAndFetchedAtBetweenOrderByFetchedAtDescIdDesc("BNBUSDT", from, to))
            .thenReturn(Optional.empty());

        Optional<TrendResponse> result = service.getTrendNotes(from, to);
        
        assertTrue(result.isEmpty());
    }

    @Test
    void returnEmptyWhenFirstAndLastAreTheSameRecord() {
        Price firstPrice = price(0, "701.05125739");
        Price lastPrice = price(0, "701.05125739");

        Instant from = Instant.parse("2026-11-01T00:00:00Z");
        Instant to = Instant.parse("2026-11-02T00:00:00Z");

        when(repository.findFirstBySymbolAndFetchedAtBetweenOrderByFetchedAtAscIdAsc("BNBUSDT", from, to))
            .thenReturn(Optional.of(firstPrice));

        when(repository.findFirstBySymbolAndFetchedAtBetweenOrderByFetchedAtDescIdDesc("BNBUSDT", from, to))
            .thenReturn(Optional.of(lastPrice));

        Optional<TrendResponse> result = service.getTrendNotes(from, to);

        assertTrue(result.isEmpty());
    }

    @Test
    void mapsPageToResponseAndTruncatesFetchedAt() {
        Pageable pageable = PageRequest.of(0, 2);
        Price p1 = price(0, "701.05125739");
        p1.setFetchedAt(Instant.parse("2026-11-01T16:00:59.987654Z"));
        Price p2 = price(1, "703.02325739");
        p2.setFetchedAt(Instant.parse("2026-11-01T16:01:59.987654Z"));
        Instant from = Instant.parse("2026-11-01T00:00:00Z");
        Instant to = Instant.parse("2026-11-02T00:00:00Z");
        Page<Price> page = new PageImpl<>(List.of(p1, p2), pageable, 3);

        when(repository.findAllBySymbolAndFetchedAtBetween("BNBUSDT", from, to, pageable))
            .thenReturn(page);

        PageablePriceResponse result = service.pageableRecords(from, to, pageable);

        assertEquals(2, result.items().size());
        assertEquals(0, result.page());
        assertEquals(2, result.size());
        assertEquals(3L, result.totalElements());
        assertTrue(result.hasNext());
        assertEquals(
            Instant.parse("2026-11-01T16:00:59Z"),
            result.items().get(0).fetchedAt()
        );
    }

    @Test
    void returnHasNextFalseWhenLastRecord() {
        Pageable pageable = PageRequest.of(1, 2);
        Price p1 = price(0, "701.05125739");
        p1.setFetchedAt(Instant.parse("2026-11-01T16:00:59.987654Z"));
        Instant from = Instant.parse("2026-11-01T00:00:00Z");
        Instant to = Instant.parse("2026-11-02T00:00:00Z");
        Page<Price> page = new PageImpl<>(List.of(p1), pageable, 3);

        when(repository.findAllBySymbolAndFetchedAtBetween("BNBUSDT", from, to, pageable))
            .thenReturn(page);

        PageablePriceResponse result = service.pageableRecords(from, to, pageable);

        assertFalse(result.hasNext());
    }

    @Test
    void returnCorrectTimeFormat() {
        Price price = price(0, "701.05125739");
        price.setFetchedAt(Instant.parse("2026-11-01T16:00:59.987654Z"));

        when(repository.findFirstBySymbolOrderByFetchedAtDesc("BNBUSDT"))
            .thenReturn(Optional.of(price));

        Optional<PriceResponse> result = service.findLatest();

        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("701.05125739"), result.get().price());
        assertEquals("BNBUSDT", result.get().symbol());
        assertEquals(
            Instant.parse("2026-11-01T16:00:59Z"),
            result.get().fetchedAt()
        );
    }

}
