package edu.ntnu.idatt2003.g23.model;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import edu.ntnu.idatt2003.g23.io.StockCsvExporter;
import edu.ntnu.idatt2003.g23.model.Stock.Volatility;
import edu.ntnu.idatt2003.g23.model.transaction.Transaction;
import edu.ntnu.idatt2003.g23.model.transaction.TransactionFactory;

/**
 * Class representing a stock exchange.
 */
public class Exchange {
  private final String name;
  private int week;
  private Map<String, Stock> stockMap;
  private Random random;
  private boolean frozen = false;

  /**
   * Transition weight matrix for volatility phases.
   * Rows/columns ordered by Volatility.ordinal():
   * 0=STABLE, 1=FAST, 2=CHAOTIC, 3=SLOW_RISE, 4=SLOW_FALL, 5=NORMAL_RISE, 6=NORMAL_FALL
   * Higher weight = more likely transition. Self-transitions are allowed for trending phases.
   */
  private static final double[][] VOLATILITY_TRANSITIONS = {
      //       ST    FA    CH    SR    SF    NR    NF
      /* ST */ {0, 5, 2, 28, 28, 18, 18},
      /* FA */ {8, 0, 10, 5, 5, 25, 25},
      /* CH */ {5, 35, 0, 5, 5, 8, 8},
      /* SR */ {15, 5, 2, 10, 20, 25, 12},  // SR can stay SR; less NR funnel
      /* SF */ {15, 5, 2, 20, 10, 12, 25},  // symmetric with SR
      /* NR */ {8, 12, 2, 18, 8, 20, 14},  // NR self-transition; less SR loop; more NF path
      /* NF */ {8, 12, 2, 8, 18, 14, 20},  // NF self-transition; less SF cushion; symmetric
  };

  /**
   * Constructor for Exchange
   *
   * @param name   of the exchange
   * @param stocks available on the exchange
   */
  public Exchange(String name, List<Stock> stocks) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Exchange name cannot be null or empty");
    }
    if (stocks == null) {
      throw new IllegalArgumentException("Stock list cannot be null");
    }

    this.name = name;
    this.week = 1;
    normalizeStockHistoryLengths(stocks);
    this.stockMap = stocks.stream().collect(Collectors.toMap(Stock::getSymbol, stock -> stock));
    this.random = new Random();
    assignVolatilities(new ArrayList<>(stocks));
  }

  /**
   * Align all stocks to the same history length by backfilling missing early weeks.
   *
   * <p>Some CSV datasets provide fewer historical points for certain stocks.
   * Without normalization, those stocks stay offset forever even if the exchange
   * advances all stocks together each week.</p>
   */
  private static void normalizeStockHistoryLengths(List<Stock> stocks) {
    if (stocks.isEmpty()) {
      return;
    }

    int maxWeeks = 0;
    for (Stock stock : stocks) {
      maxWeeks = Math.max(maxWeeks, stock.getHistoricalPrices().size());
    }

    for (Stock stock : stocks) {
      List<BigDecimal> history = stock.getHistoricalPrices();
      int missing = maxWeeks - history.size();
      if (missing <= 0) {
        continue;
      }

      BigDecimal earliestKnown = history.get(0);
      List<BigDecimal> prefix = new ArrayList<>(missing);
      for (int i = 0; i < missing; i++) {
        prefix.add(earliestKnown);
      }
      history.addAll(0, prefix);
    }
  }

  private void assignVolatilities(List<Stock> stocks) {
    int total = stocks.size();
    Collections.shuffle(stocks, random);
    int remaining = total;
    int nChaotic = Math.min(remaining, Math.max(1, (int) Math.round(total * 0.01)));
    remaining -= nChaotic;
    int nFast = Math.min(remaining, (int) Math.round(total * 0.10));
    remaining -= nFast;
    int nSlowRise = Math.min(remaining, (int) Math.round(total * 0.19));
    remaining -= nSlowRise;
    int nSlowFall = Math.min(remaining, (int) Math.round(total * 0.12));
    remaining -= nSlowFall;
    int nNormRise = Math.min(remaining, (int) Math.round(total * 0.25));
    remaining -= nNormRise;
    int nNormFall = Math.min(remaining, (int) Math.round(total * 0.18));
    remaining -= nNormFall;
    int i = 0;
    for (int c = 0; c < nChaotic; c++, i++) {
      stocks.get(i).setVolatility(Stock.Volatility.CHAOTIC);
    }
    for (int f = 0; f < nFast; f++, i++) {
      stocks.get(i).setVolatility(Stock.Volatility.FAST);
    }
    for (int r = 0; r < nSlowRise; r++, i++) {
      stocks.get(i).setVolatility(Stock.Volatility.SLOW_RISE);
    }
    for (int d = 0; d < nSlowFall; d++, i++) {
      stocks.get(i).setVolatility(Stock.Volatility.SLOW_FALL);
    }
    for (int r = 0; r < nNormRise; r++, i++) {
      stocks.get(i).setVolatility(Stock.Volatility.NORMAL_RISE);
    }
    for (int d = 0; d < nNormFall; d++, i++) {
      stocks.get(i).setVolatility(Stock.Volatility.NORMAL_FALL);
    }
    while (i < total) {
      stocks.get(i++).setVolatility(Stock.Volatility.STABLE); // Remaining stocks are stable
    }
  }

  /**
   * Gets the name of the exchange.
   *
   * @return the name of the exchange
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the current week of the exchange.
   *
   * @return the current week of the exchange
   */
  public int getWeek() {
    return week;
  }

  /**
   * Directly sets the current week. Used when restoring a saved game.
   *
   * @param week the week number to restore
   */
  public void setWeek(int week) {
    if (week < 1) {
      throw new IllegalArgumentException("Week must be positive");
    }
    this.week = week;
  }

  /**
   * Checks if stock with the given symbol exists on the exchange.
   *
   * @param symbol of the stock to check
   * @return true if stock exists, false otherwise
   */
  public boolean hasStock(String symbol) {
    if (symbol == null || symbol.isBlank()) {
      throw new IllegalArgumentException("Stock symbol cannot be null or empty");
    }
    return stockMap.containsKey(symbol);
  }

  /**
   * Gets the stock with the given symbol from the exchange.
   *
   * @param symbol of the stock to get
   * @return the stock with the given symbol
   */
  public Stock getStock(String symbol) {
    if (symbol == null || symbol.isBlank()) {
      throw new IllegalArgumentException("Stock symbol cannot be null or empty");
    }
    Stock stock = stockMap.get(symbol);
    if (stock == null) {
      throw new IllegalArgumentException("Stock with symbol " + symbol + " does not exist");
    }
    return stock;
  }

  /**
   * Finds stocks on the exchange that match the given search term in their symbol or company name.
   *
   * @param searchTerm string to search for in stock symbols and company names
   * @return a list of stocks that match the search term
   */
  public List<Stock> findStocks(String searchTerm) {
    List<Stock> result = new ArrayList<>();
    String lowerSearchTerm = searchTerm.toLowerCase();

    for (Stock stock : stockMap.values()) {
      if (stock.getSymbol().toLowerCase().contains(lowerSearchTerm) ||
          stock.getCompany().toLowerCase().contains(lowerSearchTerm)) {
        result.add(stock);
      }
    }
    return result;
  }

  /**
   * Creates a purchase transaction
   *
   * @param symbol   of the stock to buy
   * @param quantity of shares to buy
   * @param player   making the purchase
   * @return the purchase transaction
   */
  public Transaction buy(String symbol, BigDecimal quantity, Player player) {
    if (!hasStock(symbol)) {
      throw new IllegalArgumentException(
          "Stock with symbol " + symbol + " does not exist on exchange");
    }
    if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Quantity must be positive");
    }
    if (player == null) {
      throw new IllegalArgumentException("Player cannot be null");
    }

    Stock stock = getStock(symbol);
    BigDecimal currentPrice = stock.getSalesPrice();
    Share share = new Share(stock, quantity, currentPrice);
    return TransactionFactory.createPurchase(share, this.week);
  }

  /**
   * Creates a sale transaction
   *
   * @param share  to sell
   * @param player making the sale
   * @return the sale transaction
   */
  public Transaction sell(Share share, Player player) {
    if (share == null) {
      throw new IllegalArgumentException("Share cannot be null");
    }
    if (player == null) {
      throw new IllegalArgumentException("Player cannot be null");
    }

    return TransactionFactory.createSale(share, this.week);
  }

  /**
   * Freezes or unfreezes price simulation (dev mode).
   */
  public void setFrozen(boolean frozen) {
    this.frozen = frozen;
  }

  /**
   * Advances the exchange to the next week, updating stock prices based on a random percentage change.
   */
  public void advance() {
    this.week++;
    if (frozen) {
      return;
    }
    List<Stock> allStocks = new ArrayList<>(stockMap.values());
    allStocks.forEach(this::advanceStockPrice);
    applyVolatilityTransitions();
    applyAllSpikes(allStocks);
    enforceFloor(allStocks);
  }

  /** Simulates one week of price movement for a single stock. */
  private void advanceStockPrice(Stock stock) {
    BigDecimal currentPrice = stock.getSalesPrice();
    double[] range = volatilityRange(stock.getVolatility());
    double min = range[0];
    double max = range[1];

    double percentageChange = (random.nextDouble() * (max - min)) + min;
    BigDecimal multiplicativeChange = BigDecimal.valueOf(1 + (percentageChange / 100));

    boolean directional = stock.getVolatility() == Volatility.SLOW_FALL
        || stock.getVolatility() == Volatility.NORMAL_FALL
        || stock.getVolatility() == Volatility.SLOW_RISE
        || stock.getVolatility() == Volatility.NORMAL_RISE;
    if (!directional && random.nextBoolean()) {
      multiplicativeChange = BigDecimal.ONE.divide(multiplicativeChange, 4, RoundingMode.HALF_UP);
    }

    BigDecimal newPrice = currentPrice.multiply(multiplicativeChange);
    newPrice = applyMeanReversion(stock, newPrice);

    if (newPrice.compareTo(BigDecimal.valueOf(0.01)) < 0) {
      newPrice = BigDecimal.valueOf(0.01);
    }
    stock.addNewSalesPrice(newPrice);
  }

  /**
   * Returns the [min, max] percentage-change range for the given volatility phase.
   * Directional phases have a one-sided range; non-directional phases are centred near zero.
   */
  private static double[] volatilityRange(Volatility volatility) {
    return switch (volatility) {
      case SLOW_RISE -> new double[]{0.0, 4.0};
      case SLOW_FALL -> new double[]{-4.0, 0.0};
      case NORMAL_RISE -> new double[]{2.0, 5.0};
      case NORMAL_FALL -> new double[]{-5.0, -2.0};
      case FAST -> new double[]{3.0, 10.0};
      case CHAOTIC -> new double[]{7.0, 15.0};
      default -> new double[]{0.0, 3.0};
    };
  }

  /**
   * Applies log-space mean reversion toward the stock's initial price.
   * Force is proportional to log(current/initial): negligible near the start,
   * grows large enough to dominate any trend state when price diverges wildly.
   */
  private BigDecimal applyMeanReversion(Stock stock, BigDecimal price) {
    BigDecimal initialPrice = stock.getHistoricalPrices().get(0);
    double logRatio = Math.log(price.doubleValue() / initialPrice.doubleValue());
    double reversionFactor = 1.0 - logRatio * ExchangeSimulationConfig.MEAN_REVERSION_STRENGTH;
    reversionFactor = Math.max(0.50, Math.min(1.50, reversionFactor));
    return price.multiply(BigDecimal.valueOf(reversionFactor)).setScale(6, RoundingMode.HALF_UP);
  }

  /** Applies per-stock volatility phase transitions. */
  private void applyVolatilityTransitions() {
    for (Stock stock : stockMap.values()) {
      if (random.nextDouble() < 0.50) {
        stock.setVolatility(pickNextVolatility(stock.getVolatility()));
      }
    }
  }

  /** Applies all configured spike tiers for the week. */
  private void applyAllSpikes(List<Stock> stocks) {
    applySpike(stocks, 0.18, 5, 30);
    applySpike(stocks, 0.10, 10, 50);
    applySpike(stocks, 0.05, 20, 70);
    applySpike(stocks, 0.02, 30, 90);
  }

  /** Re-applies the minimum price floor after spike events. */
  private static void enforceFloor(List<Stock> stocks) {
    BigDecimal priceFloor = BigDecimal.valueOf(0.01);
    for (Stock stock : stocks) {
      if (stock.getSalesPrice().compareTo(priceFloor) < 0) {
        stock.setLatestSalesPrice(priceFloor);
      }
    }
  }

  private void applySpike(List<Stock> stocks, double chance, double minPct, double maxPct) {
    if (random.nextDouble() >= chance) {
      return;
    }
    int targets = 1;
    while (targets < ExchangeSimulationConfig.MAX_SPIKE_TARGETS_PER_EVENT
        && random.nextDouble() < ExchangeSimulationConfig.ADDITIONAL_SPIKE_TARGET_CHANCE) {
      targets++;
    }

    List<Stock> shuffled = new ArrayList<>(stocks);
    Collections.shuffle(shuffled, random);
    int affected = Math.min(targets, shuffled.size());

    for (int i = 0; i < affected; i++) {
      Stock target = shuffled.get(i);
      double pct = minPct + random.nextDouble() * (maxPct - minPct);
      BigDecimal factor = BigDecimal.valueOf(1.0 + pct / 100.0).setScale(6, RoundingMode.HALF_UP);
      BigDecimal newPrice;
      if (random.nextBoolean()) {
        newPrice = target.getSalesPrice().multiply(factor);
      } else {
        newPrice = target.getSalesPrice().divide(factor, 6, RoundingMode.HALF_UP);
      }
      target.setLatestSalesPrice(newPrice);
    }

  }

  private Volatility pickNextVolatility(Volatility current) {
    double[] weights = VOLATILITY_TRANSITIONS[current.ordinal()];
    double total = 0;
    for (double w : weights) {
      total += w;
    }
    double pick = random.nextDouble() * total;
    double cumulative = 0;
    Volatility[] vals = Volatility.values();
    for (int i = 0; i < weights.length - 1; i++) {
      cumulative += weights[i];
      if (pick < cumulative) {
        return vals[i];
      }
    }
    return vals[weights.length - 1];
  }

  /**
   * Gets the top gainers on the exchange based on their current sales price.
   *
   * @param limit the maximum number of top gainers to return
   * @return a list of the top gainers on the exchange
   * @throws IllegalArgumentException if limit is negative
   */
  public List<Stock> getGainers(int limit) {
    if (limit < 0) {
      throw new IllegalArgumentException("Limit cannot be negative");
    }
    return stockMap.values().stream()
        .sorted((s1, s2) -> s2.getSalesPrice().compareTo(s1.getSalesPrice()))
        .limit(limit)
        .collect(Collectors.toList());
  }

  /**
   * Gets the top losers on the exchange based on their current sales price.
   *
   * @param limit the maximum number of top losers to return
   * @return a list of the top losers on the exchange
   * @throws IllegalArgumentException if limit is negative
   */
  public List<Stock> getLosers(int limit) {
    if (limit < 0) {
      throw new IllegalArgumentException("Limit cannot be negative");
    }
    return stockMap.values().stream()
        .sorted((s1, s2) -> s1.getSalesPrice().compareTo(s2.getSalesPrice()))
        .limit(limit)
        .collect(Collectors.toList());
  }

  public List<Stock> getStocks() {
    return new ArrayList<>(stockMap.values());
  }

  /**
   * Exports current stock prices to a CSV file.
   *
   * @param path output file path
   * @throws IOException if writing to file fails
   */
  public void exportCurrentPrices(Path path) throws IOException {
    StockCsvExporter.writeCurrentPrices(path, stockMap.values());
  }
}
