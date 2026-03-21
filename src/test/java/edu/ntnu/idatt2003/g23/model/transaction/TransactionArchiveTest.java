package edu.ntnu.idatt2003.g23.model.transaction;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import edu.ntnu.idatt2003.g23.model.Share;
import edu.ntnu.idatt2003.g23.model.Stock;

class TransactionArchiveTest {

    @Test
    @DisplayName("Constructor creates empty archive")
    void testConstructor() {
        TransactionArchive archive = new TransactionArchive();

        assertTrue(archive.isEmpty());
        assertEquals(0, archive.getTransactions(1).size());
    }

    @Test
    @DisplayName("add returns true when adding valid transaction")
    void testAddValidTransaction() {
        TransactionArchive archive = new TransactionArchive();
        Stock stock = new Stock("AAPL", "Apple Inc.", List.of(new BigDecimal("150")));
        Share share = new Share(stock, new BigDecimal("10"), new BigDecimal("140"));
        Purchase purchase = new Purchase(share, 1);

        boolean result = archive.add(purchase);

        assertTrue(result);
        assertFalse(archive.isEmpty());
        assertEquals(1, archive.getTransactions(1).size());
    }

    @Test
    @DisplayName("add returns false when adding null transaction")
    void testAddNullTransaction() {
        TransactionArchive archive = new TransactionArchive();

        boolean result = archive.add(null);

        assertFalse(result);
        assertTrue(archive.isEmpty());
    }

    @Test
    @DisplayName("isEmpty returns true for empty archive")
    void testIsEmptyTrue() {
        TransactionArchive archive = new TransactionArchive();

        assertTrue(archive.isEmpty());
    }

    @Test
    @DisplayName("isEmpty returns false for non-empty archive")
    void testIsEmptyFalse() {
        TransactionArchive archive = new TransactionArchive();
        Stock stock = new Stock("AAPL", "Apple Inc.", List.of(new BigDecimal("150")));
        Share share = new Share(stock, new BigDecimal("10"), new BigDecimal("140"));
        Purchase purchase = new Purchase(share, 1);
        archive.add(purchase);

        assertFalse(archive.isEmpty());
    }

    @Test
    @DisplayName("getTransactions returns transactions for specific week")
    void testGetTransactions() {
        TransactionArchive archive = new TransactionArchive();
        Stock stock = new Stock("AAPL", "Apple Inc.", List.of(new BigDecimal("150")));
        Share share1 = new Share(stock, new BigDecimal("10"), new BigDecimal("140"));
        Share share2 = new Share(stock, new BigDecimal("5"), new BigDecimal("160"));

        Purchase purchase1 = new Purchase(share1, 1);
        Purchase purchase2 = new Purchase(share2, 1);
        Purchase purchase3 = new Purchase(share1, 2);

        archive.add(purchase1);
        archive.add(purchase2);
        archive.add(purchase3);

        List<Transaction> week1Transactions = archive.getTransactions(1);
        List<Transaction> week2Transactions = archive.getTransactions(2);
        List<Transaction> week3Transactions = archive.getTransactions(3);

        assertEquals(2, week1Transactions.size());
        assertTrue(week1Transactions.contains(purchase1));
        assertTrue(week1Transactions.contains(purchase2));

        assertEquals(1, week2Transactions.size());
        assertTrue(week2Transactions.contains(purchase3));

        assertEquals(0, week3Transactions.size());
    }

    @Test
    @DisplayName("getPurchases returns only purchases for specific week")
    void testGetPurchases() {
        TransactionArchive archive = new TransactionArchive();
        Stock stock = new Stock("AAPL", "Apple Inc.", List.of(new BigDecimal("150")));
        Share share = new Share(stock, new BigDecimal("10"), new BigDecimal("140"));

        Purchase purchase = new Purchase(share, 1);
        Sale sale = new Sale(share, 1);

        archive.add(purchase);
        archive.add(sale);

        List<Purchase> purchases = archive.getPurchases(1);
        List<Sale> sales = archive.getSales(1);

        assertEquals(1, purchases.size());
        assertEquals(purchase, purchases.get(0));

        assertEquals(1, sales.size());
        assertEquals(sale, sales.get(0));
    }

    @Test
    @DisplayName("getSales returns only sales for specific week")
    void testGetSales() {
        TransactionArchive archive = new TransactionArchive();
        Stock stock = new Stock("AAPL", "Apple Inc.", List.of(new BigDecimal("160")));
        Share share = new Share(stock, new BigDecimal("10"), new BigDecimal("140"));

        Purchase purchase = new Purchase(share, 1);
        Sale sale = new Sale(share, 1);

        archive.add(purchase);
        archive.add(sale);

        List<Sale> sales = archive.getSales(1);

        assertEquals(1, sales.size());
        assertEquals(sale, sales.get(0));
    }

    @Test
    @DisplayName("countDistinctWeeks returns correct count")
    void testCountDistinctWeeks() {
        TransactionArchive archive = new TransactionArchive();
        Stock stock = new Stock("AAPL", "Apple Inc.", List.of(new BigDecimal("150")));
        Share share = new Share(stock, new BigDecimal("10"), new BigDecimal("140"));

        Purchase purchase1 = new Purchase(share, 1);
        Purchase purchase2 = new Purchase(share, 2);
        Purchase purchase3 = new Purchase(share, 1); // Same week as purchase1

        archive.add(purchase1);
        archive.add(purchase2);
        archive.add(purchase3);

        assertEquals(2, archive.countDistinctWeeks());
    }

    @Test
    @DisplayName("countDistinctWeeks returns 0 for empty archive")
    void testCountDistinctWeeksEmpty() {
        TransactionArchive archive = new TransactionArchive();

        assertEquals(0, archive.countDistinctWeeks());
    }
}
