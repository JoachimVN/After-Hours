package edu.ntnu.idatt2003.g23.ui.views.game;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import edu.ntnu.idatt2003.g23.model.Exchange;
import edu.ntnu.idatt2003.g23.model.Player;
import edu.ntnu.idatt2003.g23.model.PlayerStatus;
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
    private GameViewInterface view;

    public GameController(Player player, Exchange exchange) {
        this.player = player;
        this.exchange = exchange;
        this.player.recordWeeklySnapshot(Math.max(1, exchange.getWeek()));
    }

    public void setView(GameViewInterface view) {
        this.view = view;
    }

    public List<BigDecimal> previewSell(Stock stock, BigDecimal quantityToSell) {
        BigDecimal rem = quantityToSell;
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
        BigDecimal totalquantity = BigDecimal.ZERO;

        for (Share lot : player.getPortfolio().getShares()) {
            SaleCalculator calc = new SaleCalculator(lot);

            tGross = tGross.add(calc.calculateGross());
            tFee   = tFee.add(calc.calculateCommission());
            tTax   = tTax.add(calc.calculateTax());
            totalquantity = totalquantity.add(lot.getQuantity());
        }

        return List.of(tGross, tFee, tTax, tGross.subtract(tFee).subtract(tTax), totalquantity);
    }

    public BigDecimal unitCostWithFee(Stock stock) {
        return stock.getSalesPrice().multiply(new BigDecimal("1.005"));
    }

    public int maxSellQuantity(Stock stock) {
        return player.getPortfolio().getShareBySymbol(stock.getSymbol())
            .stream().map(Share::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(0, RoundingMode.DOWN).intValue();
    }

    public BigDecimal getOwnedQuantity(String symbol) {
        return player.getPortfolio().getShareBySymbol(symbol)
                .stream().map(Share::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void handleNextWeek() {
        exchange.advance();
        player.recordWeeklySnapshot(Math.max(1, exchange.getWeek()));
        player.updateChickAvatarProgression();
    }

    public void executeSellAll() {
        Map<String, BigDecimal> quantityBySymbol = new LinkedHashMap<>();
        Map<String, Stock> stockBySymbol = new LinkedHashMap<>();

        for (Share share : new ArrayList<>(player.getPortfolio().getShares())) {
            String symbol = share.getStock().getSymbol();
            quantityBySymbol.merge(symbol, share.getQuantity(), BigDecimal::add);
            stockBySymbol.putIfAbsent(symbol, share.getStock());
        }

        BigDecimal tGross = BigDecimal.ZERO;
        BigDecimal tFee = BigDecimal.ZERO;
        BigDecimal tTax = BigDecimal.ZERO;
        BigDecimal totalquantity = BigDecimal.ZERO;

        for (Map.Entry<String, BigDecimal> entry : quantityBySymbol.entrySet()) {
            String symbol = entry.getKey();
            BigDecimal quantity = entry.getValue();
            Stock stock = stockBySymbol.get(symbol);
            List<BigDecimal> result = executeSell(stock, quantity);
            tGross = tGross.add(result.get(0));
            tFee = tFee.add(result.get(1));
            tTax = tTax.add(result.get(2));
            totalquantity = totalquantity.add(quantity);
        }
        try {
            view.showBulkReceipt("SELL ALL HOLDINGS", totalquantity, tGross.subtract(tFee).subtract(tTax), tFee, tTax, player.getMoney());
            view.updateData();
        } catch (Exception e) {
            view.showError(e.getMessage());
        }
    }

    

    public List<BigDecimal> executeSell(Stock stock, BigDecimal quantityToSell) {
        BigDecimal remaining = quantityToSell;
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
        player.recordWeeklySnapshot(Math.max(1, exchange.getWeek()));
        view.updateData();
        return List.of(tGross, tFee, tTax, tGross.subtract(tFee).subtract(tTax));
    }

    public void handleBuy(Stock stock, BigDecimal quantity) {
        BigDecimal gross = stock.getSalesPrice().multiply(quantity);
        BigDecimal fee   = gross.multiply(new BigDecimal("0.005"));
        BigDecimal total = gross.add(fee);
        view.showTradeConfirm("BUY", stock, quantity, gross, fee, BigDecimal.ZERO, total);
    }

    public void executeBuy(Stock stock, BigDecimal quantity, BigDecimal total, BigDecimal fee) {
            try {
                Transaction tx = exchange.buy(stock.getSymbol(), quantity, player);
                tx.commit(player);
                view.showReceipt("BUY", stock, quantity, total, fee, BigDecimal.ZERO, player.getMoney());
                player.recordWeeklySnapshot(Math.max(1, exchange.getWeek()));
                view.updateData();
            } catch (Exception ex) { 
                view.showError(ex.getMessage()); 
            }
    }

    public void handleSellAll(StackPane overlay) {
        BigDecimal totalOwnedquantity = player.getPortfolio().getShares().stream()
                    .map(Share::getQuantity)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalOwnedquantity.compareTo(BigDecimal.ZERO) <= 0) {
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

    // ── Model data accessors (keep view free of direct model references) ─────

    public List<Stock> getStocks() { return exchange.getStocks(); }
    public String getExchangeName() { return exchange.getName(); }
    public int getCurrentWeek() { return exchange.getWeek(); }
    public BigDecimal getPlayerCash() { return player.getMoney(); }
    public BigDecimal getPortfolioNetWorth() { return player.getPortfolio().getNetWorth(); }
    public BigDecimal getPlayerNetWorth() { return player.getNetWorth(); }
    public PlayerStatus getPlayerStatus() {
        player.calculateStatus();
        return player.getStatus();
    }
    public BigDecimal getPlayerStatusProgress() {
        return player.calculateStatusProgress();
    }
    public int getPlayerWeeksTraded() {
        return player.getWeeksTraded();
    }
    public int getPlayerWeeksTargetForNextStatus() {
        return player.getWeeksTargetForNextStatus();
    }
    public BigDecimal getPlayerWeeksProgress() {
        return player.calculateWeeksProgress();
    }
    public BigDecimal getPlayerGrowthRatio() {
        return player.getNetWorthGrowthRatio();
    }
    public BigDecimal getPlayerGrowthTargetForNextStatus() {
        return player.getGrowthTargetForNextStatus();
    }
    public BigDecimal getPlayerNetWorthProgress() {
        return player.calculateNetWorthProgress();
    }
    public List<Share> getPortfolioShares() { return player.getPortfolio().getShares(); }
    public boolean isOwned(String symbol) {
        return getOwnedQuantity(symbol).compareTo(BigDecimal.ZERO) > 0;
    }
    public int maxBuyQuantity(Stock stock) {
        BigDecimal cost = unitCostWithFee(stock);
        return player.getMoney().divide(cost, 0, RoundingMode.DOWN).max(BigDecimal.ZERO).intValue();
    }

    // ── Dev-panel helpers ─────────────────────────────────────────────────────

    public void addCash(BigDecimal amount) { player.addMoney(amount); }
    public void setCash(BigDecimal amount) {
        BigDecimal current = player.getMoney();
        if (amount.compareTo(current) > 0) player.addMoney(amount.subtract(current));
        else player.withdrawMoney(current.subtract(amount));
    }
    public void advanceWeeks(int n) {
        for (int i = 0; i < n; i++) {
            exchange.advance();
            player.recordWeeklySnapshot(Math.max(1, exchange.getWeek()));
            player.updateChickAvatarProgression();
        }
    }
    public void setFrozen(boolean frozen) { exchange.setFrozen(frozen); }

    // ── Chart trade-point data ────────────────────────────────────────────────

    public record StockTradePoint(int week, BigDecimal quantity, BigDecimal price, boolean isSell) {}

    public List<StockTradePoint> getTradePointsForStock(String symbol) {
        List<StockTradePoint> result = new ArrayList<>();
        for (var p : player.getTransactionArchive().getAllPurchases()) {
            if (p.getShare().getStock().getSymbol().equals(symbol))
                result.add(new StockTradePoint(p.getWeek(), p.getShare().getQuantity(), p.getShare().getPurchasePrice(), false));
        }
        for (var s : player.getTransactionArchive().getAllSales()) {
            if (s.getShare().getStock().getSymbol().equals(symbol))
                result.add(new StockTradePoint(s.getWeek(), s.getShare().getQuantity(), s.getShare().getPurchasePrice(), true));
        }
        return result;
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
            BigDecimal quantity   = s.getShare().getQuantity();
            BigDecimal gross = s.getCalculator().calculateGross();
            BigDecimal pricePerShare = quantity.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                    : gross.divide(quantity, 4, RoundingMode.HALF_UP);
            allTx.add(new TxRow(
                    s.getWeek(), false,
                    s.getShare().getStock().getSymbol(),
                    s.getShare().getStock().getCompany(),
                    quantity,
                    pricePerShare,
                    s.getCalculator().calculateCommission(),
                    s.getCalculator().calculateTax(),
                    s.getCalculator().calculateTotal()
            ));
        }
        allTx.sort((a, b) -> b.week() - a.week());
        return allTx;
    }

    public String getPlayerName() {
        return player.getName();
    }

    public String getPlayerAvatar() {
        return player.getDisplayedProfileAvatar();
    }

    public String getSelectedPlayerAvatar() {
        return player.getProfileAvatar();
    }

    public void setPlayerAvatar(String avatar) {
        player.setProfileAvatar(avatar);
    }

    public void setPlayerName(String name) {
        player.setName(name);
    }

    public BigDecimal getPlayerStartingMoney() {
        return player.getStartingMoney();
    }

    public int getTransactionCount() {
        return player.getTransactionArchive().getAll().size();
    }

    public record ReplayPoint(int week, BigDecimal netWorth) {}

    /**
     * Reconstructs a weekly net-worth timeline from transaction flows and historical prices.
     */
    public List<ReplayPoint> getReplaySeries() {
        List<Player.WeeklySnapshot> snapshots = player.getWeeklySnapshots();
        if (!snapshots.isEmpty()) {
            Map<Integer, BigDecimal> netWorthByWeek = new LinkedHashMap<>();
            for (Player.WeeklySnapshot s : snapshots) {
                netWorthByWeek.put(s.week(), s.netWorth());
            }
            int lastWeek = Math.max(1, exchange.getWeek());
            BigDecimal carry = player.getStartingMoney();
            List<ReplayPoint> points = new ArrayList<>();
            for (int week = 1; week <= lastWeek; week++) {
                if (netWorthByWeek.containsKey(week)) {
                    carry = netWorthByWeek.get(week);
                }
                points.add(new ReplayPoint(week, carry));
            }
            return points;
        }

        List<Transaction> tx = new ArrayList<>(player.getTransactionArchive().getAll());
        tx.sort((a, b) -> Integer.compare(a.getWeek(), b.getWeek()));

        Map<String, BigDecimal> holdings = new LinkedHashMap<>();
        BigDecimal cash = player.getStartingMoney();
        int txIndex = 0;
        int lastWeek = Math.max(1, exchange.getWeek());
        List<ReplayPoint> points = new ArrayList<>();

        for (int week = 1; week <= lastWeek; week++) {
            while (txIndex < tx.size() && tx.get(txIndex).getWeek() == week) {
                Transaction t = tx.get(txIndex++);
                String symbol = t.getShare().getStock().getSymbol();
                BigDecimal quantity = t.getShare().getQuantity();
                if (t instanceof Purchase) {
                    cash = cash.subtract(t.getCalculator().calculateTotal());
                    holdings.merge(symbol, quantity, BigDecimal::add);
                } else if (t instanceof Sale) {
                    cash = cash.add(t.getCalculator().calculateTotal());
                    BigDecimal currentQty = holdings.getOrDefault(symbol, BigDecimal.ZERO);
                    BigDecimal nextQty = currentQty.subtract(quantity);
                    if (nextQty.compareTo(BigDecimal.ZERO) <= 0) holdings.remove(symbol);
                    else holdings.put(symbol, nextQty);
                }
            }

            BigDecimal portfolioValue = BigDecimal.ZERO;
            for (var entry : holdings.entrySet()) {
                if (!exchange.hasStock(entry.getKey())) continue;
                Stock stock = exchange.getStock(entry.getKey());
                List<BigDecimal> prices = stock.getHistoricalPrices();
                if (prices.isEmpty()) continue;
                int index = Math.min(week - 1, prices.size() - 1);
                BigDecimal weekPrice = prices.get(index);
                portfolioValue = portfolioValue.add(weekPrice.multiply(entry.getValue()));
            }
            points.add(new ReplayPoint(week, cash.add(portfolioValue)));
        }
        return points;
    }

    public int getChickPhaseUnlocked() {
        return player.getChickPhaseUnlocked();
    }

    public int getWeeksUsingChickAvatar() {
        return player.getWeeksUsingChickAvatar();
    }

    public boolean isChickAvatarEquipped() {
        return player.isChickAvatarEquipped();
    }
    
}
