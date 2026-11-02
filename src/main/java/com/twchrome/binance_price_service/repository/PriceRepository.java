package com.twchrome.binance_price_service.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.twchrome.binance_price_service.entity.Price;

public interface PriceRepository extends JpaRepository<Price, Long> {

    boolean existsBySymbolAndCloseTime(String symbol, Instant closeTime);

    Optional<Price> findFirstBySymbolOrderByFetchedAtDesc(String symbol);

    @Query(
        value = "SELECT AVG(price) FROM prices WHERE symbol = :symbol AND fetched_at BETWEEN :from AND :to",
        nativeQuery=true
    )
    Optional<BigDecimal> getAveragePriceBetween(@Param("from") Instant from, @Param("to") Instant to, @Param("symbol") String symbol);

}
