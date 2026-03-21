package edu.ntnu.idatt2003.g23.model.transaction;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import edu.ntnu.idatt2003.g23.model.Player;
import edu.ntnu.idatt2003.g23.model.Share;
import edu.ntnu.idatt2003.g23.model.Stock;

class SaleTest {

    @Test
    @DisplayName("commit succeeds when player owns the share")
    void testCommitSuccess() {
        Stock stock = new Stock("AAPL", "Apple Inc.", List.of(new BigDecimal("160")));
        Share share = new Share(stock, new BigDecimal("10"), new BigDecimal("140"));
        Player player = new Player("TestPlayer", new BigDecimal("1000"));
        player.getPortfolio().addShare(share);

        Sale sale = new Sale(share, 1);

        BigDecimal initialMoney = player.getMoney();
        sale.commit(player);

        assertTrue(sale.isCommitted());
        assertEquals(0, player.getPortfolio().getShares().size());
        assertEquals(1, player.getTransactionArchive().getTransactions(1).size());
        assertEquals(sale, player.getTransactionArchive().getTransactions(1).get(0));
        assertTrue(player.getMoney().compareTo(initialMoney) > 0);
    }

    @Test
    @DisplayName("commit throws when transaction already committed")
    void testCommitThrowsWhenAlreadyCommitted() {
        Stock stock = new Stock("AAPL", "Apple Inc.", List.of(new BigDecimal("160")));
        Share share = new Share(stock, new BigDecimal("10"), new BigDecimal("140"));
        Player player = new Player("TestPlayer", new BigDecimal("1000"));
        player.getPortfolio().addShare(share);

        Sale sale = new Sale(share, 1);
        sale.commit(player);

        assertThrows(IllegalStateException.class, () -> sale.commit(player));
    }

    @Test
    @DisplayName("commit throws when player does not own the share")
    void testCommitThrowsWhenPlayerDoesNotOwnShare() {
        Stock stock = new Stock("AAPL", "Apple Inc.", List.of(new BigDecimal("160")));
        Share share = new Share(stock, new BigDecimal("10"), new BigDecimal("140"));
        Player player = new Player("TestPlayer", new BigDecimal("1000"));
        // Note: share is not added to portfolio

        Sale sale = new Sale(share, 1);

        assertThrows(IllegalStateException.class, () -> sale.commit(player));
        assertFalse(sale.isCommitted());
    }

    @Test
    @DisplayName("commit removes share from portfolio")
    void testCommitRemovesShareFromPortfolio() {
        Stock stock = new Stock("AAPL", "Apple Inc.", List.of(new BigDecimal("160")));
        Share share = new Share(stock, new BigDecimal("10"), new BigDecimal("140"));
        Player player = new Player("TestPlayer", new BigDecimal("1000"));
        player.getPortfolio().addShare(share);

        Sale sale = new Sale(share, 1);
        sale.commit(player);

        assertEquals(0, player.getPortfolio().getShares().size());
    }

    @Test
    @DisplayName("commit adds transaction to archive")
    void testCommitAddsToArchive() {
        Stock stock = new Stock("AAPL", "Apple Inc.", List.of(new BigDecimal("160")));
        Share share = new Share(stock, new BigDecimal("10"), new BigDecimal("140"));
        Player player = new Player("TestPlayer", new BigDecimal("1000"));
        player.getPortfolio().addShare(share);

        Sale sale = new Sale(share, 1);
        sale.commit(player);

        List<Transaction> transactions = player.getTransactionArchive().getTransactions(1);
        assertEquals(1, transactions.size());
        assertEquals(sale, transactions.get(0));
    }

    @Test
    @DisplayName("commit increases player's money")
    void testCommitIncreasesPlayerMoney() {
        Stock stock = new Stock("AAPL", "Apple Inc.", List.of(new BigDecimal("160")));
        Share share = new Share(stock, new BigDecimal("10"), new BigDecimal("140"));
        Player player = new Player("TestPlayer", new BigDecimal("1000"));
        player.getPortfolio().addShare(share);

        Sale sale = new Sale(share, 1);
        BigDecimal initialMoney = player.getMoney();

        sale.commit(player);

        // Player should have more money after selling at a profit
        assertTrue(player.getMoney().compareTo(initialMoney) > 0);
    }
}
