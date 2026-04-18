package edu.ntnu.idatt2003.g23.model.transaction;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import edu.ntnu.idatt2003.g23.model.Player;
import edu.ntnu.idatt2003.g23.model.Share;
import edu.ntnu.idatt2003.g23.model.Stock;

class PurchaseTest {

    private static Stock stock;
    private static Share share;

    @BeforeAll
    static void setUp() {
        stock = new Stock("AAPL", "Apple Inc.", List.of(new BigDecimal("150")));
        share = new Share(stock, new BigDecimal("10"), new BigDecimal("140"));
    }

    @Test
    @DisplayName("commit succeeds when player has enough money")
    void testCommitSuccess() {
        Player player = new Player("TestPlayer", new BigDecimal("10000"));
        Purchase purchase = new Purchase(share, 1);

        // Calculate expected cost: gross (140 * 10) + commission (1400 * 0.005) + tax (0) = 1400 + 7 = 1407
        BigDecimal expectedCost = new BigDecimal("1407");

        purchase.commit(player);

        assertTrue(purchase.isCommitted());
        assertEquals(1, player.getPortfolio().getShares().size());
        assertEquals(share, player.getPortfolio().getShares().get(0));
        assertEquals(1, player.getTransactionArchive().getTransactions(1).size());
        assertEquals(purchase, player.getTransactionArchive().getTransactions(1).get(0));
        assertEquals(0, new BigDecimal("10000").subtract(expectedCost).compareTo(player.getMoney()));
    }

    @Test
    @DisplayName("commit throws when transaction already committed")
    void testCommitThrowsWhenAlreadyCommitted() {
        Player player = new Player("TestPlayer", new BigDecimal("10000"));
        Purchase purchase = new Purchase(share, 1);
        purchase.commit(player);
        assertThrows(IllegalStateException.class, () -> purchase.commit(player));
    }

    @Test
    @DisplayName("commit throws when player does not have enough money")
    void testCommitThrowsWhenNotEnoughMoney() {
        Player player = new Player("TestPlayer", new BigDecimal("100")); // Not enough money
        Purchase purchase = new Purchase(share, 1);
        assertThrows(IllegalStateException.class, () -> purchase.commit(player));
        assertFalse(purchase.isCommitted());
    }

    @Test
    @DisplayName("commit adds transaction to archive")
    void testCommitAddsToArchive() {
        Player player = new Player("TestPlayer", new BigDecimal("10000"));
        Purchase purchase = new Purchase(share, 1);
        purchase.commit(player);
        List<Transaction> transactions = player.getTransactionArchive().getTransactions(1);
        assertEquals(1, transactions.size());
        assertEquals(purchase, transactions.get(0));
    }

    @Test
    @DisplayName("commit adds share to portfolio")
    void testCommitAddsShareToPortfolio() {
        Player player = new Player("TestPlayer", new BigDecimal("10000"));
        Purchase purchase = new Purchase(share, 1);
        purchase.commit(player);
        assertEquals(1, player.getPortfolio().getShares().size());
        assertEquals(share, player.getPortfolio().getShares().get(0));
    }

    @Test
    @DisplayName("commit throws when player is null")
    void testCommitThrowsWhenPlayerIsNull() {
        Purchase purchase = new Purchase(share, 1);
        assertThrows(IllegalArgumentException.class, () -> purchase.commit(null));
    }
}
