package edu.ntnu.idatt2003.g23.model;

import edu.ntnu.idatt2003.g23.AppConfig;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

// Represents a stock with its symbol, company name, and a list of historical prices. Provides methods to retrieve stock information and add new sales prices.
public class Stock {

  private static final BigDecimal MIN_PERCENT_BASE = new BigDecimal("0.10");

  public enum Volatility {

    // Behavior for each value is implemented in Exchange.java#advance() (see src/main/java/edu/ntnu/idatt2003/g23/model/Exchange.java)
    STABLE,
    FAST,
    CHAOTIC,
    SLOW_RISE,
    SLOW_FALL,
    NORMAL_RISE,
    NORMAL_FALL
  }

  private final String symbol;
  private final String company;
  private final List<BigDecimal> prices;
  private final Supplier<List<BigDecimal>> lazyPriceLoader;
  private boolean pricesLoaded;
  private Volatility volatility = Volatility.STABLE;

  // Constructor for creating a new Stock instance with the specified symbol, company name, and list of prices.
  public Stock(String symbol, String company, List<BigDecimal> prices) {
    if (symbol == null || symbol.isEmpty() || !symbol.matches("[A-Z]+(\\.[A-Z]+)*")) {
      throw new IllegalArgumentException("Stock symbol is null, empty, or invalid");
    }
    if (company == null || company.isEmpty()) {
      throw new IllegalArgumentException("Company name is null or empty");
    }
    if (prices == null || prices.isEmpty()) {
      throw new IllegalArgumentException("Prices list cannot be null or empty");
    }
    this.symbol = symbol;
    this.company = company;
    // Keep caller-provided list as live backing storage; tests rely on this behavior.
    this.prices = prices;
    this.lazyPriceLoader = null;
    this.pricesLoaded = true;
  }

  public Stock(String symbol, String company, Supplier<List<BigDecimal>> lazyPriceLoader) {
    if (symbol == null || symbol.isEmpty() || !symbol.matches("[A-Z]+(\\.[A-Z]+)*")) {
      throw new IllegalArgumentException("Stock symbol is null, empty, or invalid");
    }
    if (company == null || company.isEmpty()) {
      throw new IllegalArgumentException("Company name is null or empty");
    }
    if (lazyPriceLoader == null) {
      throw new IllegalArgumentException("Lazy loader cannot be null");
    }
    this.symbol = symbol;
    this.company = company;
    this.prices = new ArrayList<>();
    this.lazyPriceLoader = lazyPriceLoader;
    this.pricesLoaded = false;
  }

  private List<BigDecimal> priceData() {
    if (pricesLoaded) {
      return prices;
    }
    synchronized (prices) {
      if (!pricesLoaded) {
        List<BigDecimal> loaded = lazyPriceLoader.get();
        if (loaded == null || loaded.isEmpty()) {
          throw new IllegalStateException("Lazy price loader returned no prices for " + symbol);
        }
        prices.clear();
        prices.addAll(loaded);
        pricesLoaded = true;
      }
    }
    enforceHistoryCap(prices);
    return prices;
  }

  private static void enforceHistoryCap(List<BigDecimal> data) {
    if (!AppConfig.PERFORMANCE_MODE.get()) {
      return;
    }
    int maxWeeks = Math.max(50, AppConfig.PERFORMANCE_MAX_HISTORY_WEEKS.get());
    if (data.size() <= maxWeeks) {
      return;
    }
    int trimCount = data.size() - maxWeeks;
    data.subList(0, trimCount).clear();
  }

  /**
   * Getter for symbol
   *
   * @return the stock symbol
   */
  public String getSymbol() {
    return symbol;
  }

  /**
   * Getter for company name
   *
   * @return the company name
   */
  public String getCompany() {
    return company;
  }

  /**
   * Getter for the latest stock price
   *
   * @return the latest stock price
   */
  public BigDecimal getSalesPrice() {
    List<BigDecimal> data = priceData();
    return data.get(data.size() - 1);
  }

  /**
   * Adds a new sales price for the stock
   *
   * @param price the new sales price
   * @throws IllegalArgumentException if the new price is null or not positive
   */
  public void addNewSalesPrice(BigDecimal price) {
    if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("New price must be a positive value");
    }
    List<BigDecimal> data = priceData();
    data.add(price);
    enforceHistoryCap(data);
  }

  /**
   * Replaces the latest sales price without changing history length.
   *
   * <p>Used for same-week adjustments (for example spike events) so each week
   * still contributes exactly one data point per stock.</p>
   *
   * @param price the replacement price for the latest week
   */
  public void setLatestSalesPrice(BigDecimal price) {
    if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Latest price must be a positive value");
    }
    List<BigDecimal> data = priceData();
    data.set(data.size() - 1, price);
  }

  public Volatility getVolatility() {
    return volatility;
  }

  public void setVolatility(Volatility volatility) {
    if (volatility == null) {
      throw new IllegalArgumentException("Volatility cannot be null");
    }
    this.volatility = volatility;
  }

  /**
   * Getter for historical prices
   *
   * @return a list of historical prices for the stock
   */
  public List<BigDecimal> getHistoricalPrices() {
    return priceData();
  }

  /**
   * Gets the highest price of the stock
   *
   * @return the highest price of the stock
   */
  public BigDecimal getHighestPrice() {
    return priceData().stream().max(BigDecimal::compareTo).orElseThrow();
  }

  /**
   * Gets the lowest price of the stock
   *
   * @return the lowest price of the stock
   */
  public BigDecimal getLowestPrice() {
    return priceData().stream().min(BigDecimal::compareTo).orElseThrow();
  }

  /**
   * Calculates the latest price change
   *
   * @return the difference between the latest price and the previous price, or 0 if only one price available
   */
  public BigDecimal getLatestPriceChange() {
    List<BigDecimal> data = priceData();
    if (data.size() == 1) {
      return BigDecimal.ZERO;
    }
    return getSalesPrice().subtract(data.get(data.size() - 2));
  }

  /**
   * Calculates the percentage change from the previous price to the latest price
   *
   * @return the percentage change, or 0 if only one price available or previous price is zero
   */
  public BigDecimal percentageChange() {
    List<BigDecimal> data = priceData();
    if (data.size() < 2) {
      return BigDecimal.ZERO;
    }
    BigDecimal prev = data.get(data.size() - 2);
    BigDecimal current = data.get(data.size() - 1);
    if (prev.compareTo(BigDecimal.ZERO) == 0) {
      return BigDecimal.ZERO;
    }
    BigDecimal effectiveBase = prev.abs().max(MIN_PERCENT_BASE);
    return current.subtract(prev).divide(effectiveBase, 6, RoundingMode.HALF_UP)
        .multiply(BigDecimal.valueOf(100));
  }

  /**
   * Calculates the all-time high price of the stock
   *
   * @return the all-time high price of the stock
   */
  public BigDecimal allTimeHigh() {
    return getHistoricalPrices().stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
  }

  /**
   * Calculates the all-time low price of the stock
   *
   * @return the all-time low price of the stock
   */
  public BigDecimal allTimeLow() {
    return getHistoricalPrices().stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
  }

  /**
   * Calculates the percentage return over the last {@code weeks} weeks.
   * Pass a negative value to get the all-time return.
   *
   * @param weeks number of weeks to look back (negative = all-time)
   * @return percentage return, or 0 if insufficient price history
   */
  public BigDecimal percentageChangeOverWeeks(int weeks) {
    List<BigDecimal> data = priceData();
    if (data.size() < 2) {
      return BigDecimal.ZERO;
    }
    int fromIdx = (weeks < 0) ? 0 : Math.max(0, data.size() - 1 - weeks);
    BigDecimal from = data.get(fromIdx);
    BigDecimal to = data.get(data.size() - 1);
    if (from.compareTo(BigDecimal.ZERO) == 0) {
      return BigDecimal.ZERO;
    }
    BigDecimal effectiveBase = from.abs().max(MIN_PERCENT_BASE);
    return to.subtract(from).divide(effectiveBase, 6, RoundingMode.HALF_UP)
        .multiply(BigDecimal.valueOf(100));
  }
}