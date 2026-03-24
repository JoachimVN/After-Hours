package edu.ntnu.idatt2003.g23.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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

    @Test
    @DisplayName("getSymbol throws when symbol is null or empty")
    void testGetSymbolThrows() {
        Stock nullSymbol = new Stock(null, "Company", List.of(new BigDecimal("10")));
        Stock emptySymbol = new Stock("", "Company", List.of(new BigDecimal("10")));
        Stock intSymbol = new Stock("123", "Company", List.of(new BigDecimal("10")));

        assertThrows(IllegalStateException.class, nullSymbol::getSymbol);
        assertThrows(IllegalStateException.class, emptySymbol::getSymbol);
        assertThrows(IllegalStateException.class, intSymbol::getSymbol);
    }

    @Test
    @DisplayName("getCompany throws when company is null or empty")
    void testGetCompanyThrows() {
        Stock nullCompany = new Stock("AAPL", null, List.of(new BigDecimal("10")));
        Stock emptyCompany = new Stock("AAPL", "", List.of(new BigDecimal("10")));
        Stock intCompany = new Stock("AAPL", "123", List.of(new BigDecimal("10")));

        assertThrows(IllegalStateException.class, nullCompany::getCompany);
        assertThrows(IllegalStateException.class, emptyCompany::getCompany);
        assertDoesNotThrow(intCompany::getCompany);
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
    @DisplayName("getSalesPrice throws when no prices exist")
    void testGetSalesPriceThrows() {
        Stock stock = new Stock("AAPL", "Apple Inc.", new ArrayList<>());

        assertThrows(IllegalStateException.class, stock::getSalesPrice);
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
    @DisplayName("addNewSalesPrice throws for null or non-positive values")
    void testAddNewSalesPriceThrows() {
        Stock stock = new Stock("AAPL", "Apple Inc.", new ArrayList<>(List.of(new BigDecimal("100"))));

        assertThrows(IllegalArgumentException.class, () -> stock.addNewSalesPrice(null));
        assertThrows(IllegalArgumentException.class, () -> stock.addNewSalesPrice(BigDecimal.ZERO));
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
    @DisplayName("getHighestPrice throws when no prices exist")
    void testGetHighestPriceThrows() {
        Stock stock = new Stock("AAPL", "Apple Inc.", new ArrayList<>());
        assertThrows(IllegalStateException.class, stock::getHighestPrice);
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
    @DisplayName("getLowestPrice throws when no prices exist")
    void testGetLowestPriceThrows() {
        Stock stock = new Stock("AAPL", "Apple Inc.", new ArrayList<>());
        assertThrows(IllegalStateException.class, stock::getLowestPrice);
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

    @Test
    @DisplayName("getLatestPriceChange throws when prices is null")
    void testGetLatestPriceChangeThrowsWhenPricesNull() {
        Stock stock = new Stock("AAPL", "Apple Inc.", null);
        assertThrows(IllegalStateException.class, stock::getLatestPriceChange);
    }

    @Test
    @DisplayName("getLatestPriceChange throws when prices is empty")
    void testGetLatestPriceChangeThrowsWhenPricesEmpty() {
        Stock stock = new Stock("AAPL", "Apple Inc.", new ArrayList<>());
        assertThrows(IllegalStateException.class, stock::getLatestPriceChange);
    }
}
