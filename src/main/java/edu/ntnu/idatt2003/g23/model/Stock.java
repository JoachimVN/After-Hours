package edu.ntnu.idatt2003.g23.model;

import java.math.BigDecimal;
import java.util.List;

// Represents a stock with its symbol, company name, and a list of historical prices. Provides methods to retrieve stock information and add new sales prices.
public class Stock {
    private final String symbol;
    private final String company;
    private final List<BigDecimal> prices;

    // Constructor for creating a new Stock instance with the specified symbol, company name, and list of prices.
    public Stock(String symbol, String company, List<BigDecimal> prices) {
        this.symbol = symbol;
        this.company = company;
        this.prices = prices;
    }

    /**
     * Getter for symbol
     * @return the stock symbol
     * @throws IllegalStateException if the symbol is null, empty, or invalid
     */
    public String getSymbol() {
        if (symbol == null || symbol.isEmpty() || !symbol.matches("[A-Z]+(\\.[A-Z]+)*")) {
            throw new IllegalStateException("Stock symbol is null, empty, or invalid");
        }
        return symbol;
    }

    /**
     * Getter for company name
     * @return the company name
     * @throws IllegalStateException if the company name is null or empty
     */
    public String getCompany() {
        if (company == null || company.isEmpty()) {
            throw new IllegalStateException("Company name is null or empty");
        }
        return company;
    }

    /**
     * Getter for the latest stock price
     * @return the latest stock price
     * @throws IllegalStateException if no prices are available for the stock
     */
    public BigDecimal getSalesPrice() {
        if (prices == null || prices.isEmpty()) {
            throw new IllegalStateException("No prices available for the stock");
        }
        // Latest price = last element in the list 
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

    /**
     * Getter for historical prices
     * @return a list of historical prices for the stock
     * @throws IllegalStateException if the prices list is null
     */
    public List<BigDecimal> getHistoricalPrices() {
        if (prices == null) {
            throw new IllegalStateException("Prices cannot be null");
        }
        return prices;
    }

    /**
     * Gets the highest price of the stock
     * @return the highest price of the stock
     * @throws IllegalStateException if no prices are available for the stock
     */
    public BigDecimal getHighestPrice() {
        if (prices == null || prices.isEmpty()) {
            throw new IllegalStateException("No prices available for the stock");
        }
        return prices.stream().max(BigDecimal::compareTo).orElseThrow(() -> new IllegalStateException("No prices available for the stock"));
    }

    /**
     * Gets the lowest price of the stock
     * @return the lowest price of the stock
     * @throws IllegalStateException if no prices are available for the stock
     */
    public BigDecimal getLowestPrice() {
        if (prices == null || prices.isEmpty()) {
            throw new IllegalStateException("No prices available for the stock");
        }
        return prices.stream().min(BigDecimal::compareTo).orElseThrow(() -> new IllegalStateException("No prices available for the stock"));
    }

    /**
     * Calculates the latest price change
     * @return the difference between the latest price and the previous price, or 0 if only one price available
     * @throws IllegalStateException if no prices are available for the stock
     */
    public BigDecimal getLatestPriceChange() {
        if (prices == null) {
            throw new IllegalStateException("Prices cannot be null");
        }
        if (prices.size() == 1) {
            return BigDecimal.ZERO; // No change if there is only one price
        }
        BigDecimal latestPrice = getSalesPrice(); // prices.get(prices.size() - 1);
        BigDecimal previousPrice = prices.get(prices.size() - 2);
        return latestPrice.subtract(previousPrice);
    }
}
