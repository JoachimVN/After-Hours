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
}
