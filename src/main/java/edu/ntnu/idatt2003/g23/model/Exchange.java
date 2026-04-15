package edu.ntnu.idatt2003.g23.model;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import edu.ntnu.idatt2003.g23.io.StockCsvExporter;
import edu.ntnu.idatt2003.g23.model.transaction.Purchase;
import edu.ntnu.idatt2003.g23.model.transaction.Sale;
import edu.ntnu.idatt2003.g23.model.transaction.Transaction;

/**
 * Class representing a stock exchange.
 */
public class Exchange {
    private final String name;
    private int week;
    private Map<String, Stock> stockMap;
    private Random random;

    /**
     * Constructor for Exchange
     * @param name of the exchange
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
        this.stockMap = stocks.stream().collect(Collectors.toMap(Stock::getSymbol, stock -> stock));
        this.random = new Random();
    }

    /**
     * Gets the name of the exchange.
     * @return the name of the exchange
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the current week of the exchange.
     * @return the current week of the exchange
     */
    public int getWeek() {
        return week;
    }

    /**
     * Checks if stock with the given symbol exists on the exchange.
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
     * @param searchTerm string to search for in stock symbols and company names
     * @return a list of stocks that match the search term
     */
    public List<Stock> findStocks(String searchTerm) {
        List<Stock> result = new ArrayList<>();
        String lowerSearchTerm = searchTerm.toLowerCase();

        for (Stock stock : stockMap.values()) {
            if (stock.getSymbol().toLowerCase().contains(lowerSearchTerm) || stock.getCompany().toLowerCase().contains(lowerSearchTerm)) {
                result.add(stock);
            }
        }
        return result;
    }

    /**
     * Creates a purchase transaction
     * @param symbol of the stock to buy
     * @param quantity of shares to buy
     * @param player making the purchase
     * @return the purchase transaction
     */
    public Transaction buy(String symbol, BigDecimal quantity, Player player) {
        if (!hasStock(symbol)) {
            throw new IllegalArgumentException("Stock with symbol " + symbol + " does not exist on exchange");
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
        return new Purchase(share, this.week);
    }

    /**
     * Creates a sale transaction
     * @param share to sell
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

        return new Sale(share, this.week);
    }

    /**
     * Advances the exchange to the next week, updating stock prices based on a random percentage change.
     */
    public void advance() {
        this.week++;
        for (Stock stock : stockMap.values()) {
            BigDecimal currentPrice = stock.getSalesPrice();
            double percentageChange = (random.nextDouble() * 20) - 10;  // AI - -10% to +10%
            BigDecimal newPrice = currentPrice.multiply(BigDecimal.valueOf(1 + (percentageChange / 100)));
            stock.addNewSalesPrice(newPrice);
        }
    }

    /**
     * Gets the top gainers on the exchange based on their current sales price.
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
     * @param limit the maximum number of top losers to return
     * @throws IllegalArgumentException if limit is negative
     * @return a list of the top losers on the exchange
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
        if (stockMap == null) {
            throw new IllegalStateException("Stock map is not initialized");
        }
        return new ArrayList<>(stockMap.values());
    }

    /**
     * Exports current stock prices to a CSV file.
     * @param path output file path
     * @throws IOException if writing to file fails
     */
    public void exportCurrentPrices(Path path) throws IOException {
        StockCsvExporter.writeCurrentPrices(path, stockMap.values());
    }
}
