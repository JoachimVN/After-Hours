package edu.ntnu.idatt2003.g23.ui.views.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ntnu.idatt2003.g23.model.Player;
import edu.ntnu.idatt2003.g23.model.Share;
import edu.ntnu.idatt2003.g23.model.Stock;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GameControllerTest {

    @Test
    @DisplayName("getOwnedShares merges duplicate symbols into one holding")
    void getOwnedSharesMergesDuplicateSymbols() {
        Player player = new Player("Alice", new BigDecimal("1000.00"));
        Stock apple = new Stock("AAPL", "Apple Inc.", List.of(new BigDecimal("150.00")));
        Share firstAppleShare = new Share(apple, new BigDecimal("2"), new BigDecimal("120.00"));
        Share secondAppleShare = new Share(apple, new BigDecimal("3"), new BigDecimal("125.00"));
        player.getPortfolio().addShare(firstAppleShare);
        player.getPortfolio().addShare(secondAppleShare);

        GameController controller = new GameController(player);

        List<Share> result = controller.getOwnedShares();

        assertEquals(1, result.size());
        assertEquals("AAPL", result.getFirst().getStock().getSymbol());
        assertEquals(new BigDecimal("5"), result.getFirst().getQuantity());
        assertEquals(new BigDecimal("123.00000000"), result.getFirst().getPurchasePrice());
    }

    @Test
    @DisplayName("getOwnedShares keeps distinct symbols separate")
    void getOwnedSharesKeepsDistinctSymbolsSeparate() {
        Player player = new Player("Bob", new BigDecimal("1000.00"));
        Stock apple = new Stock("AAPL", "Apple Inc.", List.of(new BigDecimal("150.00")));
        Stock microsoft = new Stock("MSFT", "Microsoft", List.of(new BigDecimal("300.00")));
        player.getPortfolio().addShare(new Share(apple, new BigDecimal("1"), new BigDecimal("120.00")));
        player.getPortfolio().addShare(new Share(microsoft, new BigDecimal("4"), new BigDecimal("280.00")));

        GameController controller = new GameController(player);

        List<Share> result = controller.getOwnedShares();

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(share -> share.getStock().getSymbol().equals("AAPL")
                && share.getQuantity().compareTo(new BigDecimal("1")) == 0));
        assertTrue(result.stream().anyMatch(share -> share.getStock().getSymbol().equals("MSFT")
                && share.getQuantity().compareTo(new BigDecimal("4")) == 0));
    }

    @Test
    @DisplayName("getOwnedShares returns empty list when portfolio is empty")
    void getOwnedSharesReturnsEmptyListWhenNoSharesExist() {
        Player player = new Player("Charlie", new BigDecimal("1000.00"));
        GameController controller = new GameController(player);

        assertTrue(controller.getOwnedShares().isEmpty());
    }
}
