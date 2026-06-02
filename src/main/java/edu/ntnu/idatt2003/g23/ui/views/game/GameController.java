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
import edu.ntnu.idatt2003.g23.ui.util.CurrencyFormatter;

public final class GameController {

  private final Player player;
  private final Exchange exchange;
  private List<ReplayPoint> replaySeriesCache;
  private boolean replaySeriesDirty = true;
  private GameViewInterface view;
  private PlayerStatus statusOverride;
  private boolean devModeMutationsUsed = false;

  public GameController(Player player, Exchange exchange) {
    this.player = player;
    this.exchange = exchange;
    this.player.recordWeeklySnapshot(Math.max(1, exchange.getWeek()));
  }

  public void setView(GameViewInterface view) {
    this.view = view;
  }

  // AI-ASSISTED: Drafted/refined with AI support and validated by the team.
  public List<BigDecimal> previewSell(Stock stock, BigDecimal quantityToSell) {
    BigDecimal rem = quantityToSell;
    BigDecimal tGross = BigDecimal.ZERO;
    BigDecimal tFee = BigDecimal.ZERO;
    BigDecimal tTax = BigDecimal.ZERO;

    for (Share lot : player.getPortfolio().getShareBySymbol(stock.getSymbol())) {
      if (rem.compareTo(BigDecimal.ZERO) <= 0) {
        break;
      }
      BigDecimal sq = rem.min(lot.getQuantity());

      // Use SaleCalculator with a proportional slice of the lot
      Share partial = new Share(stock, sq, lot.getPurchasePrice());
      SaleCalculator calc = new SaleCalculator(partial, getPlayerTaxRate());

      tGross = tGross.add(calc.calculateGross());
      tFee = tFee.add(calc.calculateCommission());
      tTax = tTax.add(calc.calculateTax());
      rem = rem.subtract(sq);
    }
    if (rem.compareTo(BigDecimal.ZERO) > 0) {
      return null;
    }
    return List.of(tGross, tFee, tTax, tGross.subtract(tFee).subtract(tTax));
  }

  // AI-ASSISTED: Drafted/refined with AI support and validated by the team.
  public List<BigDecimal> previewSellAll() {
    BigDecimal tGross = BigDecimal.ZERO;
    BigDecimal tFee = BigDecimal.ZERO;
    BigDecimal tTax = BigDecimal.ZERO;
    BigDecimal totalquantity = BigDecimal.ZERO;

    for (Share lot : player.getPortfolio().getShares()) {
      SaleCalculator calc = new SaleCalculator(lot, getPlayerTaxRate());

      tGross = tGross.add(calc.calculateGross());
      tFee = tFee.add(calc.calculateCommission());
      tTax = tTax.add(calc.calculateTax());
      totalquantity = totalquantity.add(lot.getQuantity());
    }

    return List.of(tGross, tFee, tTax, tGross.subtract(tFee).subtract(tTax), totalquantity);
  }

  public BigDecimal unitCostWithFee(Stock stock) {
    return stock.getSalesPrice().multiply(new BigDecimal("1.005"));
  }

  // AI-ASSISTED: Drafted/refined with AI support and validated by the team.
  public int maxSellQuantity(Stock stock) {
    return player.getPortfolio().getShareBySymbol(stock.getSymbol())
        .stream().map(Share::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add)
        .setScale(0, RoundingMode.DOWN).intValue();
  }

  public BigDecimal getOwnedQuantity(String symbol) {
    return player.getPortfolio().getShareBySymbol(symbol)
        .stream().map(Share::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  // AI-ASSISTED: Drafted/refined with AI support and validated by the team.
  public void handleNextWeek() {
    exchange.advance();
    player.recordWeeklySnapshot(Math.max(1, exchange.getWeek()));
    player.updateChickAvatarProgression();
    invalidateReplaySeries();
  }

  public void scheduleTutorialMomentumNudge() {
    exchange.scheduleTutorialMomentumNudge();
  }

  // AI-ASSISTED: Drafted/refined with AI support and validated by the team.
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
      view.showBulkReceipt("SELL ALL HOLDINGS", totalquantity, tGross.subtract(tFee).subtract(tTax),
          tFee, tTax, player.getMoney());
      view.updateData();
    } catch (Exception e) {
      view.showError(e.getMessage());
    }
  }


  // AI-ASSISTED: Drafted/refined with AI support and validated by the team.
  public List<BigDecimal> executeSell(Stock stock, BigDecimal quantityToSell) {
    BigDecimal remaining = quantityToSell;
    BigDecimal tGross = BigDecimal.ZERO;
    BigDecimal tFee = BigDecimal.ZERO;
    BigDecimal tTax = BigDecimal.ZERO;
    BigDecimal sellQuantity = BigDecimal.ZERO;
    for (Share lot : new ArrayList<>(player.getPortfolio().getShareBySymbol(stock.getSymbol()))) {
      if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
        break;
      }
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
      Transaction tx = exchange.sell(sellShare, player, getPlayerTaxRate());
      tx.commit(player);
      TransactionCalculator calculator = tx.getCalculator();
      tGross = tGross.add(calculator.calculateGross());
      tFee = tFee.add(calculator.calculateCommission());
      tTax = tTax.add(calculator.calculateTax());
      remaining = remaining.subtract(sellQuantity);

    }
    view.showReceipt("SELL", stock, sellQuantity, tGross.subtract(tFee).subtract(tTax), tFee, tTax,
        player.getMoney());
    player.recordWeeklySnapshot(Math.max(1, exchange.getWeek()));
    invalidateReplaySeries();
    view.updateData();
    return List.of(tGross, tFee, tTax, tGross.subtract(tFee).subtract(tTax));
  }

  // AI-ASSISTED: Drafted/refined with AI support and validated by the team.
  public void handleBuy(Stock stock, BigDecimal quantity) {
    if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
      view.showTradeError("Enter at least 1 share to buy.");
      return;
    }

    BigDecimal gross = stock.getSalesPrice().multiply(quantity);
    BigDecimal fee = gross.multiply(new BigDecimal("0.005"));
    BigDecimal total = gross.add(fee);
    if (total.compareTo(player.getMoney()) > 0) {
      view.showTradeError("Not enough cash - costs "
          + CurrencyFormatter.format(total)
          + ", you have "
          + CurrencyFormatter.format(player.getMoney()) + ".");
      return;
    }
    view.showTradeConfirm("BUY", stock, quantity, gross, fee, BigDecimal.ZERO, total);
  }

  // AI-ASSISTED: Drafted/refined with AI support and validated by the team.
  public void executeBuy(Stock stock, BigDecimal quantity, BigDecimal total, BigDecimal fee) {
    try {
      Transaction tx = exchange.buy(stock.getSymbol(), quantity, player);
      tx.commit(player);
      view.showReceipt("BUY", stock, quantity, total, fee, BigDecimal.ZERO, player.getMoney());
      player.recordWeeklySnapshot(Math.max(1, exchange.getWeek()));
      invalidateReplaySeries();
      view.updateData();
    } catch (Exception ex) {
      view.showError(ex.getMessage());
    }
  }

  public void handleSellAll() {
    BigDecimal totalOwnedquantity = player.getPortfolio().getShares().stream()
        .map(Share::getQuantity)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    if (totalOwnedquantity.compareTo(BigDecimal.ZERO) <= 0) {
      view.showSellAllError("There is nothing to sell");
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

  public List<Stock> getStocks() {
    return exchange.getStocks();
  }

  public List<String> consumeLastSpikeSymbols() {
    return exchange.consumeLastSpikeSymbols();
  }

  public String getExchangeName() {
    return exchange.getName();
  }

  public int getCurrentWeek() {
    return exchange.getWeek();
  }

  public BigDecimal getPlayerCash() {
    return player.getMoney();
  }

  public BigDecimal getPortfolioNetWorth() {
    return player.getPortfolio().getNetWorth();
  }

  public BigDecimal getPlayerNetWorth() {
    return player.getNetWorth();
  }

  public BigDecimal getPlayerTaxRate() {
    return getPlayerStatus().getTaxRate();
  }

  // AI-ASSISTED: Drafted/refined with AI support and validated by the team.
  public PlayerStatus getPlayerStatus() {
    if (statusOverride != null) {
      return statusOverride;
    }
    player.calculateStatus();
    return player.getStatus();
  }

  public BigDecimal getPlayerStatusProgress() {
    getPlayerStatus();
    return player.calculateStatusProgress();
  }

  public BigDecimal getPlayerStatusProgress(PlayerStatus targetStatus) {
    getPlayerStatus();
    return player.calculateStatusProgress(targetStatus);
  }

  public int getPlayerWeeksTraded() {
    return player.getWeeksTraded();
  }

  public int getPlayerWeeksTargetForNextStatus() {
    getPlayerStatus();
    return player.getWeeksTargetForNextStatus();
  }

  public int getPlayerWeeksTargetForStatus(PlayerStatus targetStatus) {
    return switch (targetStatus) {
      case NOVICE -> 0;
      case INVESTOR -> PlayerStatus.NOVICE.getWeeksTargetForNextStatus();
      case SPECULATOR -> PlayerStatus.INVESTOR.getWeeksTargetForNextStatus();
    };
  }

  public BigDecimal getPlayerWeeksProgress() {
    getPlayerStatus();
    return player.calculateWeeksProgress();
  }

  public BigDecimal getPlayerWeeksProgress(PlayerStatus targetStatus) {
    getPlayerStatus();
    return player.calculateWeeksProgress(targetStatus);
  }

  public BigDecimal getPlayerGrowthRatio() {
    return player.getNetWorthGrowthRatio();
  }

  public BigDecimal getPlayerGrowthTargetForNextStatus() {
    getPlayerStatus();
    return player.getGrowthTargetForNextStatus();
  }

  public BigDecimal getPlayerGrowthTargetForStatus(PlayerStatus targetStatus) {
    return switch (targetStatus) {
      case NOVICE -> BigDecimal.ZERO;
      case INVESTOR -> PlayerStatus.NOVICE.getGrowthTargetForNextStatus();
      case SPECULATOR -> PlayerStatus.INVESTOR.getGrowthTargetForNextStatus();
    };
  }

  public BigDecimal getPlayerNetWorthProgress() {
    getPlayerStatus();
    return player.calculateNetWorthProgress();
  }

  public BigDecimal getPlayerNetWorthProgress(PlayerStatus targetStatus) {
    getPlayerStatus();
    return player.calculateNetWorthProgress(targetStatus);
  }

  // AI-ASSISTED: Drafted/refined with AI support and validated by the team.
  public List<Share> getPortfolioShares() {
    Map<String, BigDecimal> quantityBySymbol = new LinkedHashMap<>();
    Map<String, BigDecimal> costBySymbol = new LinkedHashMap<>();
    Map<String, Stock> stockBySymbol = new LinkedHashMap<>();

    for (Share share : player.getPortfolio().getShares()) {
      String symbol = share.getStock().getSymbol();
      BigDecimal quantity = share.getQuantity();
      BigDecimal lotCost = share.getPurchasePrice().multiply(quantity);

      quantityBySymbol.merge(symbol, quantity, BigDecimal::add);
      costBySymbol.merge(symbol, lotCost, BigDecimal::add);
      stockBySymbol.putIfAbsent(symbol, share.getStock());
    }

    List<Share> grouped = new ArrayList<>();
    for (Map.Entry<String, BigDecimal> entry : quantityBySymbol.entrySet()) {
      String symbol = entry.getKey();
      BigDecimal totalQuantity = entry.getValue();
      if (totalQuantity.compareTo(BigDecimal.ZERO) <= 0) {
        continue;
      }
      BigDecimal totalCost = costBySymbol.getOrDefault(symbol, BigDecimal.ZERO);
      BigDecimal avgPrice = totalCost.divide(totalQuantity, 8, RoundingMode.HALF_UP);
      grouped.add(new Share(stockBySymbol.get(symbol), totalQuantity, avgPrice));
    }
    return grouped;
  }

  public void setPlayerStatusOverride(PlayerStatus status) {
    statusOverride = status;
    devModeMutationsUsed = true;
  }

  public void clearPlayerStatusOverride() {
    statusOverride = null;
  }

  public boolean isOwned(String symbol) {
    return getOwnedQuantity(symbol).compareTo(BigDecimal.ZERO) > 0;
  }

  // AI-ASSISTED: Drafted/refined with AI support and validated by the team.
  public int maxBuyQuantity(Stock stock) {
    BigDecimal cost = unitCostWithFee(stock);
    return player.getMoney().divide(cost, 0, RoundingMode.DOWN).max(BigDecimal.ZERO).intValue();
  }

  // ── Dev-panel helpers ─────────────────────────────────────────────────────

  public void addCash(BigDecimal amount) {
    player.addMoney(amount);
    devModeMutationsUsed = true;
  }

  // AI-ASSISTED: Drafted/refined with AI support and validated by the team.
  public void setCash(BigDecimal amount) {
    BigDecimal current = player.getMoney();
    if (amount.compareTo(current) > 0) {
      player.addMoney(amount.subtract(current));
    } else {
      player.withdrawMoney(current.subtract(amount));
    }
    devModeMutationsUsed = true;
  }

  // AI-ASSISTED: Drafted/refined with AI support and validated by the team.
  public void advanceWeeks(int n) {
    for (int i = 0; i < n; i++) {
      exchange.advance();
      player.recordWeeklySnapshot(Math.max(1, exchange.getWeek()));
      player.updateChickAvatarProgression();
    }
    invalidateReplaySeries();
    devModeMutationsUsed = true;
  }

  public void setFrozen(boolean frozen) {
    exchange.setFrozen(frozen);
    devModeMutationsUsed = true;
  }

  public boolean hasDevModeMutationsUsed() {
    return devModeMutationsUsed;
  }

  // ── Chart trade-point data ────────────────────────────────────────────────

  public record StockTradePoint(int week, BigDecimal quantity, BigDecimal price, boolean isSell) {
  }

  // AI-ASSISTED: Drafted/refined with AI support and validated by the team.
  public List<StockTradePoint> getTradePointsForStock(String symbol) {
    List<StockTradePoint> result = new ArrayList<>();
    for (var p : player.getTransactionArchive().getAllPurchases()) {
      if (p.getShare().getStock().getSymbol().equals(symbol)) {
        result.add(new StockTradePoint(p.getWeek(), p.getShare().getQuantity(),
            p.getShare().getPurchasePrice(), false));
      }
    }
    for (var s : player.getTransactionArchive().getAllSales()) {
      if (s.getShare().getStock().getSymbol().equals(symbol)) {
        result.add(new StockTradePoint(s.getWeek(), s.getShare().getQuantity(),
            s.getShare().getPurchasePrice(), true));
      }
    }
    return result;
  }

  // AI-ASSISTED: Drafted/refined with AI support and validated by the team.
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
      BigDecimal quantity = s.getShare().getQuantity();
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

  public record ReplayPoint(int week, BigDecimal netWorth) {
  }

  /**
   * Reconstructs a weekly net-worth timeline from transaction flows and historical prices.
   */
  // AI-ASSISTED: Drafted/refined with AI support and validated by the team.
  public List<ReplayPoint> getReplaySeries() {
    if (!replaySeriesDirty && replaySeriesCache != null) {
      return replaySeriesCache;
    }

    List<Player.WeeklySnapshot> snapshots = player.getWeeklySnapshots();
    if (!snapshots.isEmpty()) {
      Map<Integer, BigDecimal> netWorthByWeek = new LinkedHashMap<>();
      for (Player.WeeklySnapshot s : snapshots) {
        netWorthByWeek.put(s.week(), s.netWorth());
      }
      // AI-ASSISTED: Drafted/refined with AI support and validated by the team.
      int lastWeek = Math.max(1, exchange.getWeek());
      BigDecimal carry = player.getStartingMoney();
      List<ReplayPoint> points = new ArrayList<>();
        BigDecimal prevCarry = null;
        for (int week = 1; week <= lastWeek; week++) {
          if (netWorthByWeek.containsKey(week)) {
            carry = netWorthByWeek.get(week);
          }
          if (prevCarry == null || carry.compareTo(prevCarry) != 0 || week == lastWeek) {
            points.add(new ReplayPoint(week, carry));
            prevCarry = carry;
          }
        }
        replaySeriesCache = points;
        replaySeriesDirty = false;
        return replaySeriesCache;
    }

    List<Transaction> tx = new ArrayList<>(player.getTransactionArchive().getAll());
    tx.sort((a, b) -> Integer.compare(a.getWeek(), b.getWeek()));

    Map<String, BigDecimal> holdings = new LinkedHashMap<>();
    BigDecimal cash = player.getStartingMoney();
    int txIndex = 0;
    int lastWeek = Math.max(1, exchange.getWeek());
    List<ReplayPoint> points = new ArrayList<>();
      BigDecimal prevNetWorth = null;

    // AI-ASSISTED: Drafted/refined with AI support and validated by the team.
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
          if (nextQty.compareTo(BigDecimal.ZERO) <= 0) {
            holdings.remove(symbol);
          } else {
            holdings.put(symbol, nextQty);
          }
        }
      }

      BigDecimal portfolioValue = BigDecimal.ZERO;
      for (var entry : holdings.entrySet()) {
        if (!exchange.hasStock(entry.getKey())) {
          continue;
        }
        Stock stock = exchange.getStock(entry.getKey());
        List<BigDecimal> prices = stock.getHistoricalPrices();
        if (prices.isEmpty()) {
          continue;
        }
        int index = Math.min(week - 1, prices.size() - 1);
        BigDecimal weekPrice = prices.get(index);
        portfolioValue = portfolioValue.add(weekPrice.multiply(entry.getValue()));
      }
      BigDecimal netWorth = cash.add(portfolioValue);
      if (prevNetWorth == null || netWorth.compareTo(prevNetWorth) != 0 || week == lastWeek) {
        points.add(new ReplayPoint(week, netWorth));
        prevNetWorth = netWorth;
      }
    }
    replaySeriesCache = points;
    replaySeriesDirty = false;
    return replaySeriesCache;
  }

  /**
   * Re-applies performance-mode snapshot capping and forces replay reconstruction.
   */
  // AI-ASSISTED: Drafted/refined with AI support and validated by the team.
  public void refreshReplaySeriesForSettingsChange() {
    player.setWeeklySnapshots(player.getWeeklySnapshots());
    invalidateReplaySeries();
  }

  // AI-ASSISTED: Drafted/refined with AI support and validated by the team.
  private void invalidateReplaySeries() {
    replaySeriesDirty = true;
    replaySeriesCache = null;
  }

  // AI-ASSISTED: Drafted/refined with AI support and validated by the team.
  public int getChickPhaseUnlocked() {
    return player.getChickPhaseUnlocked();
  }

  // AI-ASSISTED: Drafted/refined with AI support and validated by the team.
  public int getWeeksUsingChickAvatar() {
    return player.getWeeksUsingChickAvatar();
  }

  public boolean isChickAvatarEquipped() {
    return player.isChickAvatarEquipped();
  }

}
