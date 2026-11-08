package com.twchrome.binance_price_service.dto;

import java.util.List;

public record PageablePriceResponse(
    List<PriceResponse> items,
    int page,
    int size,
    long totalElements,
    boolean hasNext
) {

}
