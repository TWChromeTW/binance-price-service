package com.twchrome.binance_price_service;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.web.client.ResourceAccessException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.twchrome.binance_price_service.client.BinanceClient;
import com.twchrome.binance_price_service.entity.Price;
import com.twchrome.binance_price_service.repository.PriceRepository;

@SpringBootTest(
    classes = BinancePriceServiceApplication.class,
    properties = "binance.poll-interval=PT1H"
)
@AutoConfigureMockMvc
@Testcontainers
@Tag("Layer1")
@Import(PriceApiL1Test.BinanceClientStubConfiguration.class)
class PriceApiL1Test {

    private static final Instant FROM = Instant.parse("2026-11-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-11-02T00:00:00Z");

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17");

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired PriceRepository repository;

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @BeforeEach
    void clearAndSeedDatabase() {
        repository.deleteAll();
        savePrice("100.00000000", "2026-11-01T15:59:00Z", "2026-11-01T15:59:10.123Z");
        savePrice("200.00000000", "2026-11-01T16:00:00Z", "2026-11-01T16:00:20.456Z");
    }

    @Test
    void restEndpointsReadPersistedPricesFromPostgres() throws Exception {
        mockMvc.perform(get("/api/v1/prices/latest"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.symbol").value("BNBUSDT"))
            .andExpect(jsonPath("$.price").value("200.00000000"))
            .andExpect(jsonPath("$.fetchedAt").value("2026-11-01T16:00:20Z"));

        MvcResult averageResult = mockMvc.perform(get("/api/v1/prices/average")
                .param("from", FROM.toString())
                .param("to", TO.toString()))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode averageJson = objectMapper.readTree(averageResult.getResponse().getContentAsString());
        assertTrue(averageJson.get("averagePrice").isNumber());
        assertEquals(0, new BigDecimal("150.00000000").compareTo(averageJson.get("averagePrice").decimalValue()));

        mockMvc.perform(get("/api/v1/prices")
                .param("from", FROM.toString())
                .param("to", TO.toString())
                .param("page", "0")
                .param("size", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].price").value("200.00000000"))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(1))
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.hasNext").value(true));

        mockMvc.perform(get("/api/v1/prices/trend")
                .param("from", FROM.toString())
                .param("to", TO.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.symbol").value("BNBUSDT"))
            .andExpect(jsonPath("$.start_date").value("2026-11-01"))
            .andExpect(jsonPath("$.end_date").value("2026-11-02"))
            .andExpect(jsonPath("$.trend").value("UPWARD"));
    }

    private void savePrice(String priceValue, String closeTimeValue, String fetchedAtValue) {
        Price price = new Price();
        price.setSymbol("BNBUSDT");
        price.setPrice(new BigDecimal(priceValue));
        price.setCloseTime(Instant.parse(closeTimeValue));
        price.setFetchedAt(Instant.parse(fetchedAtValue));
        repository.saveAndFlush(price);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class BinanceClientStubConfiguration {

        @Bean
        @Primary
        BinanceClient testBinanceClient() {
            BinanceClient client = mock(BinanceClient.class);
            when(client.fetchAvgPrice(anyString()))
                .thenThrow(new ResourceAccessException("Binance network access is disabled in integration tests"));
            return client;
        }
    }
}
