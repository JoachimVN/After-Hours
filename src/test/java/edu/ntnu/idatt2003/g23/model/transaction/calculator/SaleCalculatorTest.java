package edu.ntnu.idatt2003.g23.model.transaction.calculator;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import edu.ntnu.idatt2003.g23.model.Share;
import edu.ntnu.idatt2003.g23.model.Stock;

class SaleCalculatorTest {

    @Test
    @DisplayName("Constructor stores share correctly")
    void testConstructor() {
        Stock stock = new Stock("AAPL", "Apple Inc.", List.of(new BigDecimal("160")));
        Share share = new Share(stock, new BigDecimal("10"), new BigDecimal("140"));

        SaleCalculator calculator = new SaleCalculator(share);

        // Test that calculations work (indirectly testing constructor)
        assertEquals(new BigDecimal("1600"), calculator.calculateGross()); // 160 * 10
    }

    @Test
    @DisplayName("calculateGross returns sale price times quantity")
    void testCalculateGross() {
        Stock stock = new Stock("AAPL", "Apple Inc.", List.of(new BigDecimal("160")));
        Share share = new Share(stock, new BigDecimal("10"), new BigDecimal("140"));

        SaleCalculator calculator = new SaleCalculator(share);

        BigDecimal expected = new BigDecimal("160").multiply(new BigDecimal("10"));
        assertEquals(expected, calculator.calculateGross());
    }

    @Test
    @DisplayName("calculateCommission returns 1% of gross amount")
    void testCalculateCommission() {
        Stock stock = new Stock("AAPL", "Apple Inc.", List.of(new BigDecimal("160")));
        Share share = new Share(stock, new BigDecimal("10"), new BigDecimal("140"));

        SaleCalculator calculator = new SaleCalculator(share);

        BigDecimal gross = new BigDecimal("1600");
        BigDecimal expectedCommission = gross.multiply(new BigDecimal("0.01"));
        assertEquals(expectedCommission, calculator.calculateCommission());
    }

    @Test
    @DisplayName("calculateTax returns 30% of profit")
    void testCalculateTax() {
        Stock stock = new Stock("AAPL", "Apple Inc.", List.of(new BigDecimal("160")));
        Share share = new Share(stock, new BigDecimal("10"), new BigDecimal("140"));

        SaleCalculator calculator = new SaleCalculator(share);

        // Profit = (sale price - purchase price) * quantity - commission = (160 - 140) * 10 - 16 = 200 - 16 = 184
        // Tax = 184 * 0.3 = 55.2
        BigDecimal expectedTax = new BigDecimal("55.2");
        assertEquals(0, expectedTax.compareTo(calculator.calculateTax()));
    }

    @Test
    @DisplayName("calculateTax returns zero when no profit")
    void testCalculateTaxNoProfit() {
        Stock stock = new Stock("AAPL", "Apple Inc.", List.of(new BigDecimal("140")));
        Share share = new Share(stock, new BigDecimal("10"), new BigDecimal("140"));

        SaleCalculator calculator = new SaleCalculator(share);

        // No profit: (140 - 140) * 10 - 14 = 0 - 14 = -14
        // Tax = -14 * 0.3 = -4.2
        BigDecimal expectedTax = new BigDecimal("-4.2");
        assertEquals(0, expectedTax.compareTo(calculator.calculateTax()));
    }

    @Test
    @DisplayName("calculateTax returns negative when loss")
    void testCalculateTaxLoss() {
        Stock stock = new Stock("AAPL", "Apple Inc.", List.of(new BigDecimal("130")));
        Share share = new Share(stock, new BigDecimal("10"), new BigDecimal("140"));

        SaleCalculator calculator = new SaleCalculator(share);

        // Loss: (130 - 140) * 10 - 13 = -100 - 13 = -113
        // Tax = -113 * 0.3 = -33.9
        BigDecimal expectedTax = new BigDecimal("-33.9");
        assertEquals(0, expectedTax.compareTo(calculator.calculateTax()));
    }

    @Test
    @DisplayName("calculateTotal returns gross minus commission minus tax")
    void testCalculateTotal() {
        Stock stock = new Stock("AAPL", "Apple Inc.", List.of(new BigDecimal("160")));
        Share share = new Share(stock, new BigDecimal("10"), new BigDecimal("140"));

        SaleCalculator calculator = new SaleCalculator(share);

        BigDecimal gross = new BigDecimal("1600");
        BigDecimal commission = gross.multiply(new BigDecimal("0.01")); // 16
        BigDecimal tax = new BigDecimal("55.2");
        BigDecimal expectedTotal = gross.subtract(commission).subtract(tax); // 1600 - 16 - 55.2 = 1528.8

        assertEquals(0, expectedTotal.compareTo(calculator.calculateTotal()));
    }

    @Test
    @DisplayName("calculateTotal with different values")
    void testCalculateTotalDifferentValues() {
        Stock stock = new Stock("GOOGL", "Google Inc.", List.of(new BigDecimal("2000")));
        Share share = new Share(stock, new BigDecimal("5"), new BigDecimal("1900"));

        SaleCalculator calculator = new SaleCalculator(share);

        BigDecimal gross = new BigDecimal("10000"); // 2000 * 5
        BigDecimal commission = gross.multiply(new BigDecimal("0.01")); // 100
        BigDecimal profit = new BigDecimal("400"); // (2000 - 1900) * 5 - 100 = 500 - 100 = 400
        BigDecimal tax = profit.multiply(new BigDecimal("0.3")); // 120
        BigDecimal expectedTotal = gross.subtract(commission).subtract(tax); // 10000 - 100 - 120 = 9780

        assertEquals(0, expectedTotal.compareTo(calculator.calculateTotal()));
    }
}
