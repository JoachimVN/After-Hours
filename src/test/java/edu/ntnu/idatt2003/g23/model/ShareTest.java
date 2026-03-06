package edu.ntnu.idatt2003.g23.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ShareTest {

    @Test
    @DisplayName("Constructor stores fields correctly")
    void testConstructorStoresValues() {
        Stock stock = new Stock("AAPL", "Apple Inc.", null);
        Share share = new Share(stock, new BigDecimal("10"), new BigDecimal("150.00"));

        assertEquals(stock, share.getStock());
        assertEquals(new BigDecimal("10"), share.getQuantity());
        assertEquals(new BigDecimal("150.00"), share.getPurchasePrice());
    }

    @Test
    @DisplayName("getStock throws when stock is null")
    void testGetStockThrows() {
        Share share = new Share(null, new BigDecimal("5"), new BigDecimal("100"));

        assertThrows(IllegalStateException.class, share::getStock);
    }

    @Test
    @DisplayName("getQuantity throws when quantity is null, zero, or negative")
    void testGetQuantityThrows() {
        Stock stock = new Stock("AAPL", "Apple Inc.", null);

        Share nullQuantity = new Share(stock, null, new BigDecimal("100"));
        Share zeroQuantity = new Share(stock, BigDecimal.ZERO, new BigDecimal("100"));
        Share negativeQuantity = new Share(stock, new BigDecimal("-5"), new BigDecimal("100"));

        assertThrows(IllegalStateException.class, nullQuantity::getQuantity);
        assertThrows(IllegalStateException.class, zeroQuantity::getQuantity);
        assertThrows(IllegalStateException.class, negativeQuantity::getQuantity);
    }

    @Test
    @DisplayName("getPurchasePrice throws when price is null, zero, or negative")
    void testGetPurchasePriceThrows() {
        Stock stock = new Stock("AAPL", "Apple Inc.", null);

        Share nullPrice = new Share(stock, new BigDecimal("10"), null);
        Share zeroPrice = new Share(stock, new BigDecimal("10"), BigDecimal.ZERO);
        Share negativePrice = new Share(stock, new BigDecimal("10"), new BigDecimal("-1"));

        assertThrows(IllegalStateException.class, nullPrice::getPurchasePrice);
        assertThrows(IllegalStateException.class, zeroPrice::getPurchasePrice);
        assertThrows(IllegalStateException.class, negativePrice::getPurchasePrice);
    }
}
