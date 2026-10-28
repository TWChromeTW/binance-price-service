package com.twchrome.binance_price_service.entity;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity 
@Table(name = "prices")
public class Price {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String symbol;

    private BigDecimal price;

    private Instant closeTime;

    private Instant fetchedAt;

    public Long getId() { return id; }
    public String getSymbol() { return symbol; }
    public BigDecimal getPrice() { return price; }
    public Instant getCloseTime() { return closeTime; }
    public Instant getFetchedAt() { return fetchedAt; }


    public void setId(Long id) { this.id = id; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public void setCloseTime(Instant closeTime) { this.closeTime = closeTime; }
    public void setFetchedAt(Instant fetchedAt) { this.fetchedAt = fetchedAt; }
}
