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

    // ── percentageChange ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("percentageChange tests")
    class PercentageChangeTests {

        @Test
        @DisplayName("Returns zero when only one price exists")
        void testSinglePrice() {
            Stock stock = new Stock("AAPL", "Apple Inc.", new ArrayList<>(List.of(new BigDecimal("100"))));
            assertEquals(0, stock.percentageChange().compareTo(BigDecimal.ZERO));
        }

        @Test
        @DisplayName("Returns zero when previous price is zero")
        void testZeroPreviousPrice() {
            Stock stock = new Stock("AAPL", "Apple Inc.",
                    new ArrayList<>(List.of(new BigDecimal("0"), new BigDecimal("100"))));
            assertEquals(0, stock.percentageChange().compareTo(BigDecimal.ZERO));
        }

        @Test
        @DisplayName("Calculates correct percentage increase")
        void testPercentageIncrease() {
            Stock stock = new Stock("AAPL", "Apple Inc.",
                    new ArrayList<>(List.of(new BigDecimal("100"), new BigDecimal("150"))));
            assertEquals(0, stock.percentageChange().compareTo(new BigDecimal("50.000000")));
        }

        @Test
        @DisplayName("Calculates correct percentage decrease")
        void testPercentageDecrease() {
            Stock stock = new Stock("AAPL", "Apple Inc.",
                    new ArrayList<>(List.of(new BigDecimal("200"), new BigDecimal("100"))));
            assertEquals(0, stock.percentageChange().compareTo(new BigDecimal("-50.000000")));
        }
    }

    // ── allTimeHigh ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("allTimeHigh tests")
    class AllTimeHighTests {

        @Test
        @DisplayName("Returns highest price from list")
        void testAllTimeHigh() {
            Stock stock = new Stock("AAPL", "Apple Inc.",
                    new ArrayList<>(List.of(new BigDecimal("80"), new BigDecimal("200"), new BigDecimal("150"))));
            assertEquals(0, stock.allTimeHigh().compareTo(new BigDecimal("200")));
        }

        @Test
        @DisplayName("Returns the only price when single entry")
        void testAllTimeHighSinglePrice() {
            Stock stock = new Stock("AAPL", "Apple Inc.", new ArrayList<>(List.of(new BigDecimal("99"))));
            assertEquals(0, stock.allTimeHigh().compareTo(new BigDecimal("99")));
        }
    }

    // ── allTimeLow ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("allTimeLow tests")
    class AllTimeLowTests {

        @Test
        @DisplayName("Returns lowest price from list")
        void testAllTimeLow() {
            Stock stock = new Stock("AAPL", "Apple Inc.",
                    new ArrayList<>(List.of(new BigDecimal("80"), new BigDecimal("200"), new BigDecimal("150"))));
            assertEquals(0, stock.allTimeLow().compareTo(new BigDecimal("80")));
        }

        @Test
        @DisplayName("Returns the only price when single entry")
        void testAllTimeLowSinglePrice() {
            Stock stock = new Stock("AAPL", "Apple Inc.", new ArrayList<>(List.of(new BigDecimal("42"))));
            assertEquals(0, stock.allTimeLow().compareTo(new BigDecimal("42")));
        }
    }

    // ── percentageChangeOverWeeks ─────────────────────────────────────────────

    @Nested
    @DisplayName("percentageChangeOverWeeks tests")
    class PercentageChangeOverWeeksTests {

        @Test
        @DisplayName("Returns zero when fewer than 2 prices exist")
        void testSinglePrice() {
            Stock stock = new Stock("AAPL", "Apple Inc.", new ArrayList<>(List.of(new BigDecimal("100"))));
            assertEquals(0, stock.percentageChangeOverWeeks(4).compareTo(BigDecimal.ZERO));
        }

        @Test
        @DisplayName("Negative weeks uses all-time start")
        void testNegativeWeeksUsesAllTime() {
            Stock stock = new Stock("AAPL", "Apple Inc.",
                    new ArrayList<>(List.of(new BigDecimal("100"), new BigDecimal("120"), new BigDecimal("150"))));
            // all-time: from 100 to 150 = +50%
            assertEquals(0, stock.percentageChangeOverWeeks(-1).compareTo(new BigDecimal("50.000000")));
        }

        @Test
        @DisplayName("Specific weeks window calculated correctly")
        void testSpecificWeeks() {
            Stock stock = new Stock("AAPL", "Apple Inc.",
                    new ArrayList<>(List.of(new BigDecimal("100"), new BigDecimal("120"), new BigDecimal("150"))));
            // 1 week back: from 120 to 150 = +25%
            assertEquals(0, stock.percentageChangeOverWeeks(1).compareTo(new BigDecimal("25.000000")));
        }

        @Test
        @DisplayName("Returns zero when from-price is zero")
        void testZeroFromPrice() {
            Stock stock = new Stock("AAPL", "Apple Inc.",
                    new ArrayList<>(List.of(new BigDecimal("0"), new BigDecimal("100"))));
            assertEquals(0, stock.percentageChangeOverWeeks(-1).compareTo(BigDecimal.ZERO));
        }

        @Test
        @DisplayName("Clamps to index 0 when weeks exceed history length")
        void testWeeksExceedHistory() {
            Stock stock = new Stock("AAPL", "Apple Inc.",
                    new ArrayList<>(List.of(new BigDecimal("100"), new BigDecimal("150"))));
            // 100 weeks back, but only 2 prices — clamps to index 0
            assertEquals(0, stock.percentageChangeOverWeeks(100).compareTo(new BigDecimal("50.000000")));
        }
    }

}
