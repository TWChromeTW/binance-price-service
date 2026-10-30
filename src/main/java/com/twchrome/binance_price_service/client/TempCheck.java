package com.twchrome.binance_price_service.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.twchrome.binance_price_service.config.BinanceProperties;

@Component
public class TempCheck implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(TempCheck.class);

    private final BinanceClient client;
    private final BinanceProperties properties;

    public TempCheck(BinanceClient client, BinanceProperties properties){
        this.client = client;
        this.properties = properties;
    }

    @Override
    public void run(String... args){
        AvgPriceResponse response = client.fetchAvgPrice(properties.symbol());
        log.info("price={}, closeTime={}", response.price(), response.closeTimeAtInstant());
    }

}
