package com.twchrome.binance_price_service.controller;

import java.time.Instant;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.twchrome.binance_price_service.dto.AveragePriceResponse;
import com.twchrome.binance_price_service.dto.PageablePriceResponse;
import com.twchrome.binance_price_service.dto.PriceResponse;
import com.twchrome.binance_price_service.dto.TrendResponse;
import com.twchrome.binance_price_service.service.PriceQueryService;



@RestController
@RequestMapping("/api/v1/prices")
public class PriceController {

    private final PriceQueryService priceQueryService;
    
    private static final int MAX_PAGE_SIZE = 1000;

    private void validateRange(Instant from, Instant to) {
        if (to.isBefore(from)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "'to' must not be before 'from'"
            );
        }
    }

    private void validatePaging(int page, int size) {
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'page' must be >= 0");
        }
        if (size <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'size' must be > 0");
        }
        if (size > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'size' must be <= " + MAX_PAGE_SIZE);
        }
    }

    public PriceController(PriceQueryService priceQueryService) {
        this.priceQueryService = priceQueryService;
    }

    @GetMapping("/latest")
    public ResponseEntity<PriceResponse> getLatestPrice() {
        return priceQueryService.findLatest()
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
    
    @GetMapping("/average")
    public ResponseEntity<AveragePriceResponse> getAvgPriceBetween(
        @RequestParam("from") Instant from,
        @RequestParam("to") Instant to
    ) {
        validateRange(from, to);

        return priceQueryService.avgPriceBetween(from, to)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
    
    @GetMapping
    public ResponseEntity<PageablePriceResponse> getPageableRecords(
        @RequestParam("from") Instant from,
        @RequestParam("to") Instant to,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        validateRange(from, to);
        validatePaging(page, size);

        Sort sort = Sort.by(Sort.Direction.DESC, "fetchedAt")
            .and(Sort.by(Sort.Direction.DESC, "id"));

        Pageable pageable = PageRequest.of(
            page,
            size,
            sort
        );

        return ResponseEntity.ok(
            priceQueryService.pageableRecords(from, to, pageable)
        );
    }
    
    @GetMapping("trend")
    public ResponseEntity<TrendResponse> getTrendResponse(
        @RequestParam("from") Instant from,
        @RequestParam("to") Instant to
    ) {
        validateRange(from, to);

        return priceQueryService.getTrendNotes(from, to)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
    

}
