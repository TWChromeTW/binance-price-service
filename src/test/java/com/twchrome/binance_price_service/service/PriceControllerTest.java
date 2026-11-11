package com.twchrome.binance_price_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


import com.twchrome.binance_price_service.controller.PriceController;
import com.twchrome.binance_price_service.dto.AveragePriceResponse;
import com.twchrome.binance_price_service.dto.PageablePriceResponse;
import com.twchrome.binance_price_service.dto.PriceResponse;
import com.twchrome.binance_price_service.dto.Trend;
import com.twchrome.binance_price_service.dto.TrendResponse;

@WebMvcTest(PriceController.class)
public class PriceControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean PriceQueryService service;

    @Test
    void latestReturnsPriceAsString() throws Exception {
        when(service.findLatest()).thenReturn(
            Optional.of(new PriceResponse(
                "BNBUSDT",
                new BigDecimal("698.53476581"),
                Instant.parse("2026-11-08T15:59:58Z")
            ))
        );

        mockMvc.perform(get("/api/v1/prices/latest"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.price").isString())
            .andExpect(jsonPath("$.fetchedAt").value("2026-11-08T15:59:58Z"));
    }

    @Test
    void latestReturnsOptionalEmpty() throws Exception {
        when(service.findLatest())
            .thenReturn(Optional.empty());
        
        mockMvc.perform(get("/api/v1/prices/latest"))
            .andExpect(status().isNotFound());
    }

    @Test
    void averageReturnsPriceAsNumber() throws Exception {
        Instant from = Instant.parse("2026-11-01T00:00:00Z");
        Instant to = Instant.parse("2026-11-02T00:00:00Z");

        when(service.avgPriceBetween(from, to))
            .thenReturn(Optional.of(new AveragePriceResponse(new BigDecimal("698.53245849"))));

        mockMvc.perform(get("/api/v1/prices/average")
            .param("from", from.toString())
            .param("to", to.toString())
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.averagePrice").isNumber());
    }

    @Test
    void averageReturnsBadRequestWhenFromIsHigherThenTo() throws Exception { 
        Instant from = Instant.parse("2026-11-02T00:00:00Z");
        Instant to = Instant.parse("2026-11-01T00:00:00Z");

        mockMvc.perform(get("/api/v1/prices/average")
            .param("from", from.toString())
            .param("to", to.toString())
            )
            .andExpect(status().isBadRequest());
    }

    @Test
    void trendReturnsCorrectStringNames() throws Exception {
        Instant from = Instant.parse("2026-11-01T00:00:00Z");
        Instant to = Instant.parse("2026-11-02T00:00:00Z");

        when(service.getTrendNotes(from, to))
            .thenReturn(Optional.of(new TrendResponse(
                "BNBUSDT",
                Instant.parse("2026-11-01T00:00:00Z"),
                Instant.parse("2026-11-02T00:00:00Z"),
                Trend.UPWARD
            )));

        mockMvc.perform(get("/api/v1/prices/trend")
            .param("from", from.toString())
            .param("to", to.toString())
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.symbol").value("BNBUSDT"))
            .andExpect(jsonPath("$.start_date").isString())
            .andExpect(jsonPath("$.start_date").value("2026-11-01"))
            .andExpect(jsonPath("$.end_date").isString())
            .andExpect(jsonPath("$.end_date").value("2026-11-02"))
            .andExpect(jsonPath("$.trend").value("UPWARD"));
    }

    @Test
    void trendReturnBadRequestWhenFromIsHigherThenTo() throws Exception {
        Instant from = Instant.parse("2026-11-02T00:00:00Z");
        Instant to = Instant.parse("2026-11-01T00:00:00Z");

        mockMvc.perform(get("/api/v1/prices/trend")
            .param("from", from.toString())
            .param("to", to.toString())
            )
            .andExpect(status().isBadRequest());
    }

    @Test
    void pageReturnsItemsAndMetadata() throws Exception {
        Instant from = Instant.parse("2026-11-01T00:00:00Z");
        Instant to = Instant.parse("2026-11-02T00:00:00Z");
        PageablePriceResponse response = new PageablePriceResponse(
            List.of(new PriceResponse(
                "BNBUSDT",
                new BigDecimal("698.53476581"),
                Instant.parse("2026-11-01T15:59:58Z")
            )),
            1,
            2,
            5,
            true
        );
        when(service.pageableRecords(eq(from), eq(to), any(Pageable.class)))
            .thenReturn(response);

        mockMvc.perform(get("/api/v1/prices")
                .param("from", from.toString())
                .param("to", to.toString())
                .param("page", "1")
                .param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].symbol").value("BNBUSDT"))
            .andExpect(jsonPath("$.items[0].price").isString())
            .andExpect(jsonPath("$.page").value(1))
            .andExpect(jsonPath("$.size").value(2))
            .andExpect(jsonPath("$.totalElements").value(5))
            .andExpect(jsonPath("$.hasNext").value(true));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(service).pageableRecords(eq(from), eq(to), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        assertEquals(1, pageable.getPageNumber());
        assertEquals(2, pageable.getPageSize());
        assertEquals(
            List.of(Sort.Order.desc("fetchedAt"), Sort.Order.desc("id")),
            pageable.getSort().stream().toList()
        );
    }

    @Test
    void pageUsesDefaultPageAndSizeWhenOmitted() throws Exception {
        Instant from = Instant.parse("2026-11-01T00:00:00Z");
        Instant to = Instant.parse("2026-11-02T00:00:00Z");
        when(service.pageableRecords(eq(from), eq(to), any(Pageable.class)))
            .thenReturn(new PageablePriceResponse(List.of(), 0, 20, 0, false));

        mockMvc.perform(get("/api/v1/prices")
                .param("from", from.toString())
                .param("to", to.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(0))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalElements").value(0))
            .andExpect(jsonPath("$.hasNext").value(false));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(service).pageableRecords(eq(from), eq(to), pageableCaptor.capture());
        assertEquals(0, pageableCaptor.getValue().getPageNumber());
        assertEquals(20, pageableCaptor.getValue().getPageSize());
    }

    @Test
    void pageRejectsInvalidPageAndSize() throws Exception {
        String from = "2026-11-01T00:00:00Z";
        String to = "2026-11-02T00:00:00Z";

        mockMvc.perform(get("/api/v1/prices")
                .param("from", from)
                .param("to", to)
                .param("page", "-1"))
            .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/prices")
                .param("from", from)
                .param("to", to)
                .param("size", "0"))
            .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/prices")
                .param("from", from)
                .param("to", to)
                .param("size", "1001"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void pageRejectsReversedDateRange() throws Exception {
        mockMvc.perform(get("/api/v1/prices")
                .param("from", "2026-11-02T00:00:00Z")
                .param("to", "2026-11-01T00:00:00Z"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void pageRejectsMalformedFromDate() throws Exception {
        mockMvc.perform(get("/api/v1/prices")
                .param("from", "abc")
                .param("to", "2026-11-02T00:00:00Z"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }
}
