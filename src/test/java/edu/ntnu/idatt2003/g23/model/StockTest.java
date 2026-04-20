package edu.ntnu.idatt2003.g23.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

class StockTest {

    @Test
    @DisplayName("Constructor stores fields correctly")
    void testConstructorStoresValues() {
        List<BigDecimal> prices = new ArrayList<>(List.of(new BigDecimal("100.50")));
        Stock stock = new Stock("AAPL", "Apple Inc.", prices);

        assertEquals("AAPL", stock.getSymbol());
        assertEquals("Apple Inc.", stock.getCompany());
        assertEquals(new BigDecimal("100.50"), stock.getSalesPrice());
    }

    @Nested
    @DisplayName("Constructor validation tests")
    class ConstructorValidationTests {

        @Test
        @DisplayName("Constructor throws on null symbol")
        void testConstructorThrowsOnNullSymbol() {
            List<BigDecimal> prices = List.of(new BigDecimal("10"));
            assertThrows(IllegalArgumentException.class, () -> new Stock(null, "Company", prices));
        }

        @Test
        @DisplayName("Constructor throws on empty symbol")
        void testConstructorThrowsOnEmptySymbol() {
            List<BigDecimal> prices = List.of(new BigDecimal("10"));
            assertThrows(IllegalArgumentException.class, () -> new Stock("", "Company", prices));
        }

        @Test
        @DisplayName("Constructor throws on invalid symbol format")
        void testConstructorThrowsOnInvalidSymbolFormat() {
            List<BigDecimal> prices = List.of(new BigDecimal("10"));
            assertThrows(IllegalArgumentException.class, () -> new Stock("123", "Company", prices));
        }

        @Test
        @DisplayName("Constructor throws on null company")
        void testConstructorThrowsOnNullCompany() {
            List<BigDecimal> prices = List.of(new BigDecimal("10"));
            assertThrows(IllegalArgumentException.class, () -> new Stock("AAPL", null, prices));
        }

        @Test
        @DisplayName("Constructor throws on empty company")
        void testConstructorThrowsOnEmptyCompany() {
            List<BigDecimal> prices = List.of(new BigDecimal("10"));
            assertThrows(IllegalArgumentException.class, () -> new Stock("AAPL", "", prices));
        }

        @Test
        @DisplayName("Constructor throws on null prices")
        void testConstructorThrowsOnNullPrices() {
            assertThrows(IllegalArgumentException.class, () -> new Stock("AAPL", "Apple Inc.", null));
        }

        @Test
        @DisplayName("Constructor throws on empty prices")
        void testConstructorThrowsOnEmptyPrices() {
            assertThrows(IllegalArgumentException.class, () -> new Stock("AAPL", "Apple Inc.", new ArrayList<>()));
        }
    }

    @Test
    @DisplayName("getSalesPrice returns last price in list")
    void testGetSalesPrice() {
        List<BigDecimal> prices = new ArrayList<>(List.of(
                new BigDecimal("100"),
                new BigDecimal("0"),
                new BigDecimal("-10")
        ));
        Stock stock = new Stock("AAPL", "Apple Inc.", prices);

        assertEquals(new BigDecimal("-10"), stock.getSalesPrice());
    }

    @Test
    @DisplayName("addNewSalesPrice adds a valid price")
    void testAddNewSalesPrice() {
        List<BigDecimal> prices = new ArrayList<>(List.of(new BigDecimal("100")));
        Stock stock = new Stock("AAPL", "Apple Inc.", prices);

        stock.addNewSalesPrice(new BigDecimal("120"));

        assertEquals(new BigDecimal("120"), stock.getSalesPrice());
        assertEquals(2, prices.size());
    }

    @Test
    @DisplayName("addNewSalesPrice throws on null price")
    void testAddNewSalesPriceThrowsOnNull() {
        Stock stock = new Stock("AAPL", "Apple Inc.", new ArrayList<>(List.of(new BigDecimal("100"))));
        assertThrows(IllegalArgumentException.class, () -> stock.addNewSalesPrice(null));
    }

    @Test
    @DisplayName("addNewSalesPrice throws on zero price")
    void testAddNewSalesPriceThrowsOnZero() {
        Stock stock = new Stock("AAPL", "Apple Inc.", new ArrayList<>(List.of(new BigDecimal("100"))));
        assertThrows(IllegalArgumentException.class, () -> stock.addNewSalesPrice(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("addNewSalesPrice throws on negative price")
    void testAddNewSalesPriceThrowsOnNegative() {
        Stock stock = new Stock("AAPL", "Apple Inc.", new ArrayList<>(List.of(new BigDecimal("100"))));
        assertThrows(IllegalArgumentException.class, () -> stock.addNewSalesPrice(new BigDecimal("-5")));
    }

    @Test
    @DisplayName("getHighestPrice returns highest value")
    void testGetHighestPrice() {
        Stock stock = new Stock(
                "AAPL",
                "Apple Inc.",
                new ArrayList<>(List.of(
                        new BigDecimal("100.50"),
                        new BigDecimal("99.99"),
                        new BigDecimal("123.45")
                ))
        );

        assertEquals(new BigDecimal("123.45"), stock.getHighestPrice());
    }

    @Test
    @DisplayName("getLowestPrice returns lowest value")
    void testGetLowestPrice() {
        Stock stock = new Stock(
                "AAPL",
                "Apple Inc.",
                new ArrayList<>(List.of(
                        new BigDecimal("100.50"),
                        new BigDecimal("99.99"),
                        new BigDecimal("123.45")
                ))
        );

        assertEquals(new BigDecimal("99.99"), stock.getLowestPrice());
    }

    @Test
    @DisplayName("getVolatility returns default STABLE")
    void testGetVolatilityDefault() {
        Stock stock = new Stock("AAPL", "Apple Inc.", new ArrayList<>(List.of(new BigDecimal("100"))));

        assertEquals(Stock.Volatility.STABLE, stock.getVolatility());
    }

    @Test
    @DisplayName("setVolatility updates the volatility")
    void testSetVolatility() {
        Stock stock = new Stock("AAPL", "Apple Inc.", new ArrayList<>(List.of(new BigDecimal("100"))));

        stock.setVolatility(Stock.Volatility.CHAOTIC);

        assertEquals(Stock.Volatility.CHAOTIC, stock.getVolatility());
    }

    @Test
    @DisplayName("setVolatility throws on null")
    void testSetVolatilityThrowsOnNull() {
        Stock stock = new Stock("AAPL", "Apple Inc.", new ArrayList<>(List.of(new BigDecimal("100"))));

        assertThrows(IllegalArgumentException.class, () -> stock.setVolatility(null));
    }

    @Test
    @DisplayName("getHistoricalPrices returns the full prices list")
    void testGetHistoricalPrices() {
        List<BigDecimal> prices = new ArrayList<>(List.of(new BigDecimal("100"), new BigDecimal("110")));
        Stock stock = new Stock("AAPL", "Apple Inc.", prices);

        assertEquals(prices, stock.getHistoricalPrices());
    }

    @Test
    @DisplayName("getLatestPriceChange returns zero when only one price exists")
    void testGetLatestPriceChangeSinglePrice() {
        Stock stock = new Stock(
                "AAPL",
                "Apple Inc.",
                new ArrayList<>(List.of(new BigDecimal("100.00")))
        );

        assertEquals(BigDecimal.ZERO, stock.getLatestPriceChange());
    }

    @Test
    @DisplayName("getLatestPriceChange returns latest minus previous")
    void testGetLatestPriceChange() {
        Stock stock = new Stock(
                "AAPL",
                "Apple Inc.",
                new ArrayList<>(List.of(
                        new BigDecimal("100.00"),
                        new BigDecimal("110.50")
                ))
        );

        assertEquals(new BigDecimal("10.50"), stock.getLatestPriceChange());
    }

}
