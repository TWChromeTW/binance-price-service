package com.twchrome.binance_price_service.repository;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;

import com.twchrome.binance_price_service.entity.Price;

public interface PriceRepository extends JpaRepository<Price, Long> {

    boolean existsBySymbolAndCloseTime(String symbol, Instant closeTime);

}
