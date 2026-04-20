package edu.ntnu.idatt2003.g23.ui.views.game;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import edu.ntnu.idatt2003.g23.model.Exchange;
import edu.ntnu.idatt2003.g23.model.Player;
import edu.ntnu.idatt2003.g23.model.Share;
import edu.ntnu.idatt2003.g23.model.Stock;
import edu.ntnu.idatt2003.g23.model.transaction.calculator.SaleCalculator;

public final class GameController {

    private final Player player;
    private final Exchange exchange;
    private final GameView view;

    public GameController(Player player, Exchange exchange, GameView view) {
        this.player = player;
        this.exchange = exchange;
        this.view = view;
    }

    public List<BigDecimal> previewSell(Stock stock, BigDecimal qtyToSell) {
        BigDecimal rem = qtyToSell;
        BigDecimal tGross = BigDecimal.ZERO, tFee = BigDecimal.ZERO, tTax = BigDecimal.ZERO;

        for (Share lot : player.getPortfolio().getShareBySymbol(stock.getSymbol())) {
            if (rem.compareTo(BigDecimal.ZERO) <= 0) break;
            BigDecimal sq = rem.min(lot.getQuantity());

            // Use SaleCalculator with a proportional slice of the lot
            Share partial = new Share(stock, sq, lot.getPurchasePrice());
            SaleCalculator calc = new SaleCalculator(partial);

            tGross = tGross.add(calc.calculateGross());
            tFee   = tFee.add(calc.calculateCommission());
            tTax   = tTax.add(calc.calculateTax());
            rem    = rem.subtract(sq);
        }
        if (rem.compareTo(BigDecimal.ZERO) > 0) return null;
        return List.of(tGross, tFee, tTax, tGross.subtract(tFee).subtract(tTax));
    }

    public BigDecimal unitCostWithFee(Stock stock) {
        return stock.getSalesPrice().multiply(new BigDecimal("1.005"));
    }

    public int maxSellQuantity(Stock stock) {
        return player.getPortfolio().getShareBySymbol(stock.getSymbol())
            .stream().map(Share::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(0, RoundingMode.DOWN).intValue();
}

}
