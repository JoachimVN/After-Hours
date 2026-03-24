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
        Portfolio portfolio = new Portfolio();

        assertEquals(shares, portfolio.getShares());
    }

    @Test
    @DisplayName("addShare adds a valid share")
    void testAddShare() {
        Portfolio portfolio = new Portfolio();

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
        Portfolio portfolio = new Portfolio();

        assertThrows(IllegalArgumentException.class, () -> portfolio.addShare(null));
    }

    @Test
    @DisplayName("removeShare removes an existing share")
    void testRemoveShare() {
        Stock stock = new Stock("AAPL", "Apple Inc.", null);
        Share share = new Share(stock, new BigDecimal("5"), new BigDecimal("100"));

        Portfolio portfolio = new Portfolio();
        portfolio.addShare(share);

        boolean result = portfolio.removeShare(share);

        assertTrue(result);
        assertTrue(portfolio.getShares().isEmpty());
    }

    @Test
    @DisplayName("removeShare throws when share is null")
    void testRemoveShareThrows() {
        Portfolio portfolio = new Portfolio();

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

        Portfolio portfolio = new Portfolio();
        portfolio.addShare(s1);
        portfolio.addShare(s2);
        portfolio.addShare(s3);

        List<Share> result = portfolio.getShareBySymbol("AAPL");

        assertEquals(2, result.size());
        assertTrue(result.contains(s1));
        assertTrue(result.contains(s3));
        assertFalse(result.contains(s2));
    }

    @Test
    @DisplayName("getShareBySymbol throws when symbol is null")
    void testGetShareBySymbolThrows() {
        Portfolio portfolio = new Portfolio();

        assertThrows(IllegalArgumentException.class, () -> portfolio.getShareBySymbol(null));
    }

    @Test
    @DisplayName("getNetWorth calculates total value from SaleCalculator totals")
    void testGetNetWorth() {
        Stock apple = new Stock("AAPL", "Apple Inc.", List.of(new BigDecimal("120")));
        Stock tesla = new Stock("TSLA", "Tesla Inc.", List.of(new BigDecimal("70")));

        Share appleShare = new Share(apple, new BigDecimal("10"), new BigDecimal("100"));
        Share teslaShare = new Share(tesla, new BigDecimal("2"), new BigDecimal("50"));

        Portfolio portfolio = new Portfolio();
        portfolio.addShare(appleShare);
        portfolio.addShare(teslaShare);

        assertEquals(new BigDecimal("1258.620"), portfolio.getNetWorth());
    }

    @Test
    @DisplayName("getNetWorth is zero for empty portfolio")
    void testGetNetWorthEmptyPortfolio() {
        Portfolio portfolio = new Portfolio();

        assertEquals(BigDecimal.ZERO, portfolio.getNetWorth());
    }
}
