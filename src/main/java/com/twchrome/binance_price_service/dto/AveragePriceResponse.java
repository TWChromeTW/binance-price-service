package com.twchrome.binance_price_service.dto;

import java.math.BigDecimal;

public record AveragePriceResponse(
    BigDecimal averagePrice
) {

}
