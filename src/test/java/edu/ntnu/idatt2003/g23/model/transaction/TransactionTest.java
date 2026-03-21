package edu.ntnu.idatt2003.g23.model.transaction;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import edu.ntnu.idatt2003.g23.model.Share;
import edu.ntnu.idatt2003.g23.model.Stock;
import edu.ntnu.idatt2003.g23.model.transaction.calculator.PurchaseCalculator;
import edu.ntnu.idatt2003.g23.model.transaction.calculator.TransactionCalculator;

class TransactionTest {

    // Mock implementation of Transaction for testing abstract class
    private static class TestTransaction extends Transaction {
        public TestTransaction(Share share, int week, TransactionCalculator calculator) {
            super(share, week, calculator);
        }

        @Override
        public void commit(edu.ntnu.idatt2003.g23.model.Player player) {
            // Mock implementation
        }
    }

    @Test
    @DisplayName("Constructor stores values correctly")
    void testConstructorStoresValues() {
        Stock stock = new Stock("AAPL", "Apple Inc.", List.of(new BigDecimal("150")));
        Share share = new Share(stock, new BigDecimal("10"), new BigDecimal("140"));
        TransactionCalculator calculator = new PurchaseCalculator(share);
        int week = 5;

        Transaction transaction = new TestTransaction(share, week, calculator);

        assertEquals(share, transaction.getShare());
        assertEquals(week, transaction.getWeek());
        assertEquals(calculator, transaction.getCalculator());
        assertFalse(transaction.isCommitted());
    }

    @Test
    @DisplayName("getShare returns correct share")
    void testGetShare() {
        Stock stock = new Stock("AAPL", "Apple Inc.", List.of(new BigDecimal("150")));
        Share share = new Share(stock, new BigDecimal("10"), new BigDecimal("140"));
        TransactionCalculator calculator = new PurchaseCalculator(share);

        Transaction transaction = new TestTransaction(share, 1, calculator);

        assertEquals(share, transaction.getShare());
    }

    @Test
    @DisplayName("getWeek returns correct week")
    void testGetWeek() {
        Stock stock = new Stock("AAPL", "Apple Inc.", List.of(new BigDecimal("150")));
        Share share = new Share(stock, new BigDecimal("10"), new BigDecimal("140"));
        TransactionCalculator calculator = new PurchaseCalculator(share);
        int week = 42;

        Transaction transaction = new TestTransaction(share, week, calculator);

        assertEquals(week, transaction.getWeek());
    }

    @Test
    @DisplayName("getCalculator returns correct calculator")
    void testGetCalculator() {
        Stock stock = new Stock("AAPL", "Apple Inc.", List.of(new BigDecimal("150")));
        Share share = new Share(stock, new BigDecimal("10"), new BigDecimal("140"));
        TransactionCalculator calculator = new PurchaseCalculator(share);

        Transaction transaction = new TestTransaction(share, 1, calculator);

        assertEquals(calculator, transaction.getCalculator());
    }
}
