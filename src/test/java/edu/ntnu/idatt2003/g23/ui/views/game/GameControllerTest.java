package edu.ntnu.idatt2003.g23.ui.views.game;

import edu.ntnu.idatt2003.g23.model.Exchange;
import edu.ntnu.idatt2003.g23.model.Player;
import edu.ntnu.idatt2003.g23.model.Stock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link GameController} using a no-op stub for
 * {@link GameViewInterface} — no Mockito, no JavaFX.
 */
class GameControllerTest {

    /** A no-op stub that records the last error/message for assertion. */
    static class StubView implements GameViewInterface {
        String lastError;
        String lastReceiptAction;
        String lastConfirmAction;
        boolean updateCalled;

        @Override public void updateData() { updateCalled = true; }
        @Override public void showError(String message) { lastError = message; }
        @Override public void showTradeConfirm(String action, Stock stock, BigDecimal quantity,
                BigDecimal gross, BigDecimal fee, BigDecimal tax, BigDecimal total) {
            lastConfirmAction = action;
        }
        @Override public void showBulkTradeConfirm(String action, BigDecimal quantity,
                BigDecimal gross, BigDecimal fee, BigDecimal tax, BigDecimal total) {
            lastConfirmAction = action;
        }
        @Override public void showReceipt(String action, Stock stock, BigDecimal quantity,
                BigDecimal total, BigDecimal fee, BigDecimal tax, BigDecimal newCash) {
            lastReceiptAction = action;
        }
        @Override public void showBulkReceipt(String action, BigDecimal quantity,
                BigDecimal total, BigDecimal fee, BigDecimal tax, BigDecimal newCash) {
            lastReceiptAction = action;
        }
    }

    private Stock stock;
    private Player player;
    private Exchange exchange;
    private GameController controller;
    private StubView view;

    @BeforeEach
    void setUp() {
        stock = new Stock("AAPL", "Apple Inc.",
                new ArrayList<>(List.of(new BigDecimal("100"), new BigDecimal("105"), new BigDecimal("110"))));
        player = new Player("Test", new BigDecimal("10000"));
        exchange = new Exchange("TestMarket", List.of(stock));
        controller = new GameController(player, exchange);
        view = new StubView();
        controller.setView(view);
    }

    // ── Pure computation ──────────────────────────────────────────────────────

    @Test
    void getPlayerCash_returnsInitialBalance() {
        assertEquals(new BigDecimal("10000"), controller.getPlayerCash());
    }

    @Test
    void getExchangeName_returnsName() {
        assertEquals("TestMarket", controller.getExchangeName());
    }

    @Test
    void getCurrentWeek_startsAtOne() {
        assertEquals(1, controller.getCurrentWeek());
    }

    @Test
    void getStocks_returnsExchangeStocks() {
        assertEquals(1, controller.getStocks().size());
        assertEquals("AAPL", controller.getStocks().get(0).getSymbol());
    }

    @Test
    void unitCostWithFee_includesHalfPercent() {
        BigDecimal expected = stock.getSalesPrice().multiply(new BigDecimal("1.005"));
        assertEquals(0, expected.compareTo(controller.unitCostWithFee(stock)));
    }

    @Test
    void maxSellQuantity_withNoShares_isZero() {
        assertEquals(0, controller.maxSellQuantity(stock));
    }

    @Test
    void maxBuyQuantity_isFloorOfCashDividedByCostPlusFee() {
        BigDecimal cost = stock.getSalesPrice().multiply(new BigDecimal("1.005"));
        int expected = player.getMoney()
                .divide(cost, 0, java.math.RoundingMode.DOWN).intValue();
        assertEquals(expected, controller.maxBuyQuantity(stock));
    }

    @Test
    void isOwned_withNoShares_isFalse() {
        assertFalse(controller.isOwned("AAPL"));
    }

    @Test
    void getOwnedQuantity_withNoShares_isZero() {
        assertEquals(0, controller.getOwnedQuantity("AAPL").compareTo(BigDecimal.ZERO));
    }

    @Test
    void previewSell_withNoOwnedShares_returnsNull() {
        assertNull(controller.previewSell(stock, BigDecimal.ONE));
    }

    @Test
    void previewSellAll_withNoShares_returnsZeroTotals() {
        List<BigDecimal> preview = controller.previewSellAll();
        assertEquals(5, preview.size());
        for (BigDecimal v : preview) assertEquals(0, BigDecimal.ZERO.compareTo(v));
    }

    @Test
    void handleNextWeek_advancesWeek() {
        controller.handleNextWeek();
        assertEquals(2, controller.getCurrentWeek());
    }

    @Test
    void advanceWeeks_advancesMultiple() {
        controller.advanceWeeks(3);
        assertEquals(4, controller.getCurrentWeek());
    }

    @Test
    void addCash_increasesBalance() {
        controller.addCash(new BigDecimal("500"));
        assertEquals(0, new BigDecimal("10500").compareTo(controller.getPlayerCash()));
    }

    @Test
    void setCash_toHigherAmount_increases() {
        controller.setCash(new BigDecimal("20000"));
        assertEquals(0, new BigDecimal("20000").compareTo(controller.getPlayerCash()));
    }

    @Test
    void setCash_toLowerAmount_decreases() {
        controller.setCash(new BigDecimal("5000"));
        assertEquals(0, new BigDecimal("5000").compareTo(controller.getPlayerCash()));
    }

    @Test
    void getPortfolioNetWorth_withNoShares_isZero() {
        assertEquals(0, BigDecimal.ZERO.compareTo(controller.getPortfolioNetWorth()));
    }

    @Test
    void getTransactionHistory_withNoTransactions_isEmpty() {
        assertTrue(controller.getTransactionHistory().isEmpty());
    }

    @Test
    void getTradePointsForStock_withNoTransactions_isEmpty() {
        assertTrue(controller.getTradePointsForStock("AAPL").isEmpty());
    }

    // ── View-dependent methods ────────────────────────────────────────────────

    @Test
    void handleBuy_showsTradeConfirmOnView() {
        controller.handleBuy(stock, new BigDecimal("2"));
        assertEquals("BUY", view.lastConfirmAction);
    }

    @Test
    void executeBuy_updatesViewOnSuccess() {
        controller.executeBuy(stock, BigDecimal.ONE,
                stock.getSalesPrice().multiply(new BigDecimal("1.005")),
                stock.getSalesPrice().multiply(new BigDecimal("0.005")));
        assertTrue(view.updateCalled);
        assertEquals("BUY", view.lastReceiptAction);
    }

    @Test
    void executeBuy_playerOwnsShareAfterPurchase() {
        controller.executeBuy(stock, new BigDecimal("2"),
                stock.getSalesPrice().multiply(new BigDecimal("2")).multiply(new BigDecimal("1.005")),
                stock.getSalesPrice().multiply(new BigDecimal("2")).multiply(new BigDecimal("0.005")));
        assertTrue(controller.isOwned("AAPL"));
        assertEquals(0, new BigDecimal("2").compareTo(controller.getOwnedQuantity("AAPL")));
    }

    @Test
    void executeSell_afterBuy_updatesViewWithSellReceipt() {
        controller.executeBuy(stock, BigDecimal.ONE,
                stock.getSalesPrice().multiply(new BigDecimal("1.005")),
                stock.getSalesPrice().multiply(new BigDecimal("0.005")));
        controller.executeSell(stock, BigDecimal.ONE);
        assertEquals("SELL", view.lastReceiptAction);
    }

    @Test
    void handleSellAll_withNoShares_showsError() {
        controller.handleSellAll(null);
        assertNotNull(view.lastError);
    }

    @Test
    void handleSellAll_withShares_showsBulkTradeConfirm() {
        controller.executeBuy(stock, BigDecimal.ONE,
                stock.getSalesPrice().multiply(new BigDecimal("1.005")),
                stock.getSalesPrice().multiply(new BigDecimal("0.005")));
        view.lastConfirmAction = null;
        controller.handleSellAll(null);
        assertEquals("SELL ALL HOLDINGS", view.lastConfirmAction);
    }
}
