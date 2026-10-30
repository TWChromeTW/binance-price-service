package com.twchrome.binance_price_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan 
@SpringBootApplication
public class BinancePriceServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(BinancePriceServiceApplication.class, args);
	}

}
