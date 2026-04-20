package edu.ntnu.idatt2003.g23.ui.views.game;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import edu.ntnu.idatt2003.g23.model.Exchange;
import edu.ntnu.idatt2003.g23.model.Player;
import edu.ntnu.idatt2003.g23.model.Share;
import edu.ntnu.idatt2003.g23.model.Stock;
import edu.ntnu.idatt2003.g23.model.transaction.Purchase;
import edu.ntnu.idatt2003.g23.model.transaction.Sale;
import edu.ntnu.idatt2003.g23.model.transaction.Transaction;
import edu.ntnu.idatt2003.g23.model.transaction.calculator.SaleCalculator;
import edu.ntnu.idatt2003.g23.model.transaction.calculator.TransactionCalculator;
import javafx.scene.layout.StackPane;

public final class GameController {

    private final Player player;
    private final Exchange exchange;
    private final GameViewNew view;

    public GameController(Player player, Exchange exchange, GameViewNew view) {
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

    public List<BigDecimal> previewSellAll() {
        BigDecimal tGross = BigDecimal.ZERO;
        BigDecimal tFee = BigDecimal.ZERO;
        BigDecimal tTax = BigDecimal.ZERO;
        BigDecimal totalQty = BigDecimal.ZERO;

        for (Share lot : player.getPortfolio().getShares()) {
            SaleCalculator calc = new SaleCalculator(lot);

            tGross = tGross.add(calc.calculateGross());
            tFee   = tFee.add(calc.calculateCommission());
            tTax   = tTax.add(calc.calculateTax());
            totalQty = totalQty.add(lot.getQuantity());
        }

        return List.of(tGross, tFee, tTax, tGross.subtract(tFee).subtract(tTax), totalQty);
    }

    public BigDecimal unitCostWithFee(Stock stock) {
        return stock.getSalesPrice().multiply(new BigDecimal("1.005"));
    }

    public int maxSellQuantity(Stock stock) {
        return player.getPortfolio().getShareBySymbol(stock.getSymbol())
            .stream().map(Share::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(0, RoundingMode.DOWN).intValue();
    }

    public void handleNextWeek() {
        exchange.advance();
    }

    public void executeSellAll() {
        Map<String, BigDecimal> qtyBySymbol = new LinkedHashMap<>();
        Map<String, Stock> stockBySymbol = new LinkedHashMap<>();

        for (Share share : new ArrayList<>(player.getPortfolio().getShares())) {
            String symbol = share.getStock().getSymbol();
            qtyBySymbol.merge(symbol, share.getQuantity(), BigDecimal::add);
            stockBySymbol.putIfAbsent(symbol, share.getStock());
        }

        BigDecimal tGross = BigDecimal.ZERO;
        BigDecimal tFee = BigDecimal.ZERO;
        BigDecimal tTax = BigDecimal.ZERO;
        BigDecimal totalQty = BigDecimal.ZERO;

        for (Map.Entry<String, BigDecimal> entry : qtyBySymbol.entrySet()) {
            String symbol = entry.getKey();
            BigDecimal qty = entry.getValue();
            Stock stock = stockBySymbol.get(symbol);
            List<BigDecimal> result = executeSell(stock, qty);
            tGross = tGross.add(result.get(0));
            tFee = tFee.add(result.get(1));
            tTax = tTax.add(result.get(2));
            totalQty = totalQty.add(qty);
        }
        try {
            view.showBulkReceipt("SELL ALL HOLDINGS", totalQty, tGross.subtract(tFee).subtract(tTax), tFee, tTax, player.getMoney());
            view.updateData();
        } catch (Exception e) {
            view.showError(e.getMessage());
        }
    }

    

    public List<BigDecimal> executeSell(Stock stock, BigDecimal qtyToSell) {
        BigDecimal remaining = qtyToSell;
        BigDecimal tGross = BigDecimal.ZERO, tFee = BigDecimal.ZERO, tTax = BigDecimal.ZERO;
        BigDecimal sellQuantity = BigDecimal.ZERO;
        for (Share lot : new ArrayList<>(player.getPortfolio().getShareBySymbol(stock.getSymbol()))) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
            sellQuantity = remaining.min(lot.getQuantity());
            Share sellShare;
            if (sellQuantity.compareTo(lot.getQuantity()) < 0) {
                BigDecimal leftover = lot.getQuantity().subtract(sellQuantity);
                player.getPortfolio().removeShare(lot);
                sellShare = new Share(stock, sellQuantity, lot.getPurchasePrice());
                player.getPortfolio().addShare(sellShare);
                player.getPortfolio().addShare(new Share(stock, leftover, lot.getPurchasePrice()));
            } else {
                sellShare = lot;
            }
            Transaction tx = exchange.sell(sellShare, player);
            tx.commit(player);
            TransactionCalculator calculator = tx.getCalculator();
            tGross = tGross.add(calculator.calculateGross()); 
            tFee = tFee.add(calculator.calculateCommission()); 
            tTax = tTax.add(calculator.calculateTax());
            remaining = remaining.subtract(sellQuantity);

        }
        view.showReceipt("SELL", stock, sellQuantity, tGross.subtract(tFee).subtract(tTax), tFee, tTax, player.getMoney());
        view.updateData();
        return List.of(tGross, tFee, tTax, tGross.subtract(tFee).subtract(tTax));
    }

    public void handleBuy(Stock stock, BigDecimal quantity) {
        BigDecimal gross = stock.getSalesPrice().multiply(quantity);
        BigDecimal fee   = gross.multiply(new BigDecimal("0.005"));
        BigDecimal total = gross.add(fee);
        view.showTradeConfirm("BUY", stock, quantity, gross, fee, BigDecimal.ZERO, total);
    };

    public void executeBuy(Stock stock, BigDecimal quantity, BigDecimal total, BigDecimal fee) {
            try {
                Transaction tx = exchange.buy(stock.getSymbol(), quantity, player);
                tx.commit(player);
                view.showReceipt("BUY", stock, quantity, total, fee, BigDecimal.ZERO, player.getMoney());
                view.updateData();
            } catch (Exception ex) { 
                view.showError(ex.getMessage()); 
            }
    };

    public void handleSellAll(StackPane overlay) {
        BigDecimal totalOwnedQty = player.getPortfolio().getShares().stream()
                    .map(Share::getQuantity)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalOwnedQty.compareTo(BigDecimal.ZERO) <= 0) {
            view.showError("You don't own any shares to sell.");
            return;
        }

        List<BigDecimal> preview = previewSellAll();
        view.showBulkTradeConfirm(
                "SELL ALL HOLDINGS",
                preview.get(4),
                preview.get(0),
                preview.get(1),
                preview.get(2),
                preview.get(3));
    }

    public List<TxRow> getTransactionHistory() {
        List<TxRow> allTx = new ArrayList<>();
        for (Purchase p : player.getTransactionArchive().getAllPurchases()) {
            allTx.add(new TxRow(
                    p.getWeek(), true,
                    p.getShare().getStock().getSymbol(),
                    p.getShare().getStock().getCompany(),
                    p.getShare().getQuantity(),
                    p.getShare().getPurchasePrice(),
                    p.getCalculator().calculateCommission(),
                    BigDecimal.ZERO,
                    p.getCalculator().calculateTotal()
            ));
        }
        for (Sale s : player.getTransactionArchive().getAllSales()) {
            BigDecimal qty   = s.getShare().getQuantity();
            BigDecimal gross = s.getCalculator().calculateGross();
            BigDecimal pricePerShare = qty.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                    : gross.divide(qty, 4, RoundingMode.HALF_UP);
            allTx.add(new TxRow(
                    s.getWeek(), false,
                    s.getShare().getStock().getSymbol(),
                    s.getShare().getStock().getCompany(),
                    qty,
                    pricePerShare,
                    s.getCalculator().calculateCommission(),
                    s.getCalculator().calculateTax(),
                    s.getCalculator().calculateTotal()
            ));
        }
        allTx.sort((a, b) -> b.week() - a.week());
        return allTx;
    }
    
}
