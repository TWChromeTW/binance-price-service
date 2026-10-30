package com.twchrome.binance_price_service.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.twchrome.binance_price_service.config.BinanceProperties;

@Component 
public class BinanceClient {

    private final RestClient client;

    public BinanceClient(RestClient.Builder builder, BinanceProperties properties){
        client = builder
            .baseUrl(properties.baseUrl())
            .build();
    }

    public AvgPriceResponse fetchAvgPrice(String symbol){
        return client.get()
                .uri(UriBuilder -> UriBuilder
                        .path("/api/v3/avgPrice")
                        .queryParam("symbol", symbol)
                        .build()
                    )
                .retrieve()
                .body(AvgPriceResponse.class);
    }

}
