package edu.ntnu.idatt2003.g23.ui.views.game;

import edu.ntnu.idatt2003.g23.model.Player;
import edu.ntnu.idatt2003.g23.model.Share;
import edu.ntnu.idatt2003.g23.model.Stock;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GameController {
    private final Player player;

    public GameController(Player player) {
        this.player = player;
    }

    public List<Share> getOwnedShares() {
        Map<String, ShareSummary> sharesBySymbol = new LinkedHashMap<>();

        for (Share share : player.getPortfolio().getShares()) {
            String symbol = share.getStock().getSymbol();
            ShareSummary summary = sharesBySymbol.computeIfAbsent(
                    symbol,
                    unused -> new ShareSummary(share.getStock()));
            summary.add(share);
        }

        return sharesBySymbol.values().stream()
                .map(ShareSummary::toShare)
                .toList();
    }

    private static final class ShareSummary {
        private final Stock stock;
        private BigDecimal quantity = BigDecimal.ZERO;
        private BigDecimal totalCost = BigDecimal.ZERO;

        private ShareSummary(Stock stock) {
            this.stock = stock;
        }

        private void add(Share share) {
            quantity = quantity.add(share.getQuantity());
            totalCost = totalCost.add(share.getPurchasePrice().multiply(share.getQuantity()));
        }

        private Share toShare() {
            BigDecimal averagePurchasePrice = totalCost.divide(quantity, 8, RoundingMode.HALF_UP);
            return new Share(stock, quantity, averagePurchasePrice);
        }
    }
}
