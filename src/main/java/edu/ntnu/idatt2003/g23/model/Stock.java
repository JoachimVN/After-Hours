package edu.ntnu.idatt2003.g23.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

// Represents a stock with its symbol, company name, and a list of historical prices. Provides methods to retrieve stock information and add new sales prices.
public class Stock {

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
        this.prices = prices;
    }

    /**
     * Getter for symbol
     * @return the stock symbol
     */
    public String getSymbol() {
        return symbol;
    }

    /**
     * Getter for company name
     * @return the company name
     */
    public String getCompany() {
        return company;
    }

    /**
     * Getter for the latest stock price
     * @return the latest stock price
     */
    public BigDecimal getSalesPrice() {
        return prices.get(prices.size() - 1);
    }

    /**
     * Adds a new sales price for the stock
     * @param price the new sales price
     * @throws IllegalArgumentException if the new price is null or not positive
     */
    public void addNewSalesPrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("New price must be a positive value");
        }
        prices.add(price);
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
     * @return a list of historical prices for the stock
     */
    public List<BigDecimal> getHistoricalPrices() {
        return prices;
    }

    /**
     * Gets the highest price of the stock
     * @return the highest price of the stock
     */
    public BigDecimal getHighestPrice() {
        return prices.stream().max(BigDecimal::compareTo).orElseThrow();
    }

    /**
     * Gets the lowest price of the stock
     * @return the lowest price of the stock
     */
    public BigDecimal getLowestPrice() {
        return prices.stream().min(BigDecimal::compareTo).orElseThrow();
    }

    /**
     * Calculates the latest price change
     * @return the difference between the latest price and the previous price, or 0 if only one price available
     */
    public BigDecimal getLatestPriceChange() {
        if (prices.size() == 1) {
            return BigDecimal.ZERO;
        }
        return getSalesPrice().subtract(prices.get(prices.size() - 2));
    }

    /**
     * Calculates the percentage change from the previous price to the latest price
     * @return the percentage change, or 0 if only one price available or previous price is zero
     */
    public BigDecimal percentageChange() {
        if (prices.size() < 2) return BigDecimal.ZERO;
        BigDecimal prev    = prices.get(prices.size() - 2);
        BigDecimal current = prices.get(prices.size() - 1);
        if (prev.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return current.subtract(prev).divide(prev, 6, RoundingMode.HALF_UP)
                      .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Calculates the all-time high price of the stock
     * @return the all-time high price of the stock
     */
    public BigDecimal allTimeHigh() {
        return getHistoricalPrices().stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
    }

    /**
     * Calculates the all-time low price of the stock
     * @return the all-time low price of the stock
     */
    public BigDecimal allTimeLow() {
        return getHistoricalPrices().stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
    }

    /**
     * Calculates the percentage return over the last {@code weeks} weeks.
     * Pass a negative value to get the all-time return.
     * @param weeks number of weeks to look back (negative = all-time)
     * @return percentage return, or 0 if insufficient price history
     */
    public BigDecimal percentageChangeOverWeeks(int weeks) {
        if (prices.size() < 2) return BigDecimal.ZERO;
        int fromIdx = (weeks < 0) ? 0 : Math.max(0, prices.size() - 1 - weeks);
        BigDecimal from = prices.get(fromIdx);
        BigDecimal to   = prices.get(prices.size() - 1);
        if (from.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return to.subtract(from).divide(from, 6, RoundingMode.HALF_UP)
                 .multiply(BigDecimal.valueOf(100));
    }
}