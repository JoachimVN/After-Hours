package edu.ntnu.idatt2003.g23.model.transaction;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import edu.ntnu.idatt2003.g23.model.Share;
import edu.ntnu.idatt2003.g23.model.Stock;

class TransactionFactoryTest {

    @Test
    @DisplayName("createPurchase returns a Purchase transaction")
    void testCreatePurchase() {
        Stock stock = new Stock("AAPL", "Apple Inc.", List.of(new BigDecimal("150")));
        Share share = new Share(stock, new BigDecimal("10"), new BigDecimal("150"));

        Transaction transaction = TransactionFactory.createPurchase(share, 1);

        assertNotNull(transaction);
        assertTrue(transaction instanceof Purchase);
        assertEquals(share, transaction.getShare());
        assertEquals(1, transaction.getWeek());
    }

    @Test
    @DisplayName("createSale returns a Sale transaction")
    void testCreateSale() {
        Stock stock = new Stock("AAPL", "Apple Inc.", List.of(new BigDecimal("150")));
        Share share = new Share(stock, new BigDecimal("10"), new BigDecimal("150"));

        Transaction transaction = TransactionFactory.createSale(share, 2);

        assertNotNull(transaction);
        assertTrue(transaction instanceof Sale);
        assertEquals(share, transaction.getShare());
        assertEquals(2, transaction.getWeek());
    }

    @Test
    @DisplayName("createTransaction throws if type is null")
    void testCreateTransactionNullType() {
        Stock stock = new Stock("AAPL", "Apple Inc.", List.of(new BigDecimal("150")));
        Share share = new Share(stock, new BigDecimal("10"), new BigDecimal("150"));

        assertThrows(IllegalArgumentException.class, () -> TransactionFactory.createTransaction(null, share, 1));
    }

    @Test
    @DisplayName("createTransaction returns correct type")
    void testCreateTransactionType() {
        Stock stock = new Stock("AAPL", "Apple Inc.", List.of(new BigDecimal("150")));
        Share share = new Share(stock, new BigDecimal("10"), new BigDecimal("150"));

        Transaction purchase = TransactionFactory.createTransaction(TransactionFactory.TransactionType.PURCHASE, share, 1);
        assertTrue(purchase instanceof Purchase);

        Transaction sale = TransactionFactory.createTransaction(TransactionFactory.TransactionType.SALE, share, 1);
        assertTrue(sale instanceof Sale);
    }
}