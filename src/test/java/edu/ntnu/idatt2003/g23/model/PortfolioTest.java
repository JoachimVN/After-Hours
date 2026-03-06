package edu.ntnu.idatt2003.g23.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PortfolioTest {

    @Test
    @DisplayName("Constructor stores list reference correctly")
    void testConstructorStoresValues() {
        List<Share> shares = new ArrayList<>();
        Portfolio portfolio = new Portfolio(shares);

        assertEquals(shares, portfolio.getShares());
    }

    @Test
    @DisplayName("addShare adds a valid share")
    void testAddShare() {
        Portfolio portfolio = new Portfolio(new ArrayList<>());

        Stock stock = new Stock("AAPL", "Apple Inc.", null);
        Share share = new Share(stock, new BigDecimal("10"), new BigDecimal("150"));

        boolean result = portfolio.addShare(share);

        assertTrue(result);
        assertEquals(1, portfolio.getShares().size());
        assertEquals(share, portfolio.getShares().get(0));
    }

    @Test
    @DisplayName("addShare throws when share is null")
    void testAddShareThrows() {
        Portfolio portfolio = new Portfolio(new ArrayList<>());

        assertThrows(IllegalArgumentException.class, () -> portfolio.addShare(null));
    }

    @Test
    @DisplayName("removeShare removes an existing share")
    void testRemoveShare() {
        Stock stock = new Stock("AAPL", "Apple Inc.", null);
        Share share = new Share(stock, new BigDecimal("5"), new BigDecimal("100"));

        List<Share> shares = new ArrayList<>(List.of(share));
        Portfolio portfolio = new Portfolio(shares);

        boolean result = portfolio.removeShare(share);

        assertTrue(result);
        assertTrue(portfolio.getShares().isEmpty());
    }

    @Test
    @DisplayName("removeShare throws when share is null")
    void testRemoveShareThrows() {
        Portfolio portfolio = new Portfolio(new ArrayList<>());

        assertThrows(IllegalArgumentException.class, () -> portfolio.removeShare(null));
    }

    @Test
    @DisplayName("getShareBySymbol returns matching shares")
    void testGetShareBySymbol() {
        Stock apple = new Stock("AAPL", "Apple Inc.", null);
        Stock tesla = new Stock("TSLA", "Tesla Inc.", null);

        Share s1 = new Share(apple, new BigDecimal("10"), new BigDecimal("150"));
        Share s2 = new Share(tesla, new BigDecimal("3"), new BigDecimal("700"));
        Share s3 = new Share(apple, new BigDecimal("2"), new BigDecimal("155"));

        Portfolio portfolio = new Portfolio(new ArrayList<>(List.of(s1, s2, s3)));

        List<Share> result = portfolio.getShareBySymbol("AAPL");

        assertEquals(2, result.size());
        assertTrue(result.contains(s1));
        assertTrue(result.contains(s3));
    }

    @Test
    @DisplayName("getShareBySymbol throws when symbol is null")
    void testGetShareBySymbolThrows() {
        Portfolio portfolio = new Portfolio(new ArrayList<>());

        assertThrows(IllegalArgumentException.class, () -> portfolio.getShareBySymbol(null));
    }
}
