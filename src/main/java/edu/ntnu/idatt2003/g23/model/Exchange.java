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

    /**
     * Transition weight matrix for volatility phases.
     * Rows/columns ordered by Volatility.ordinal():
     * 0=STABLE, 1=FAST, 2=CHAOTIC, 3=SLOW_RISE, 4=SLOW_FALL, 5=NORMAL_RISE, 6=NORMAL_FALL
     * Higher weight = more likely transition. Diagonal is 0 (no self-transition).
     */
    private static final double[][] VOLATILITY_TRANSITIONS = {
        //       ST    FA    CH    SR    SF    NR    NF
        /* ST */ {  0,   5,   2,  30,  30,  15,  15 },
        /* FA */ {  8,   0,  10,   5,   5,  25,  25 },
        /* CH */ {  5,  35,   0,   5,   5,   8,   8 },
        /* SR */ { 20,   5,   3,   0,  12,  35,   5 },
        /* SF */ { 20,   5,   3,  12,   0,   5,  35 },
        /* NR */ { 10,  18,   3,  28,   5,   0,   8 },
        /* NF */ { 10,  18,   3,   5,  28,   8,   0 },
    };

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
        assignVolatilities(new ArrayList<>(stocks));
    }

    private void assignVolatilities(List<Stock> stocks) {
        int total = stocks.size();
        Collections.shuffle(stocks, random);
        int nChaotic   = Math.max(1, (int) Math.round(total * 0.01)); // 2% of total stocks, ensure at least 1 chaotic stock
        int nFast      = (int) Math.round(total * 0.10); // 10% of total stocks
        int nSlowRise  = (int) Math.round(total * 0.19); // 18% of total stocks
        int nSlowFall  = (int) Math.round(total * 0.12); // 12% of total stocks
        int nNormRise  = (int) Math.round(total * 0.25); // 25% of total stocks
        int nNormFall  = (int) Math.round(total * 0.18); // 18% of total stocks
        // total percentage assigned: 85%, remaining 15% will be stable
        int i = 0;
        for (int c = 0; c < nChaotic  && i < total; c++, i++) stocks.get(i).setVolatility(Stock.Volatility.CHAOTIC);
        for (int f = 0; f < nFast     && i < total; f++, i++) stocks.get(i).setVolatility(Stock.Volatility.FAST);
        for (int r = 0; r < nSlowRise && i < total; r++, i++) stocks.get(i).setVolatility(Stock.Volatility.SLOW_RISE);
        for (int d = 0; d < nSlowFall && i < total; d++, i++) stocks.get(i).setVolatility(Stock.Volatility.SLOW_FALL);
        for (int r = 0; r < nNormRise && i < total; r++, i++) stocks.get(i).setVolatility(Stock.Volatility.NORMAL_RISE);
        for (int d = 0; d < nNormFall && i < total; d++, i++) stocks.get(i).setVolatility(Stock.Volatility.NORMAL_FALL);
        while (i < total) stocks.get(i++).setVolatility(Stock.Volatility.STABLE); // Remaining stocks are stable
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
        return TransactionFactory.createPurchase(share, this.week);
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

        Stock stock = share.getStock();
        return TransactionFactory.createSale(share, this.week);
    }

    /**
     * Advances the exchange to the next week, updating stock prices based on a random percentage change.
     */
    public void advance() {
        this.week++;
        for (Stock stock : stockMap.values()) {
            BigDecimal currentPrice = stock.getSalesPrice();

            double min, max;
            switch (stock.getVolatility()) {
                case SLOW_RISE   -> { min =  0.0; max =  4.0; } // 18%:
                case SLOW_FALL   -> { min = -4.0; max =  0.0; } // 12%: 
                case NORMAL_RISE -> { min =  2.0; max = 5.0; } // 25%: 
                case NORMAL_FALL -> { min = -5.0; max = -2.0; } // 18%: 
                case FAST        -> { min =  3.0; max = 10.0; } // 10%: 
                case CHAOTIC     -> { min = 7.0; max = 15.0; } //  1%: 
                default          -> { min =  0.0; max =  3.0; } // 15%: 
            }

            double percentageChange = (random.nextDouble() * (max - min)) + min;
            BigDecimal multiplicativeChange = BigDecimal.valueOf(1 + (percentageChange / 100));  // 1 to 1.10

            boolean randomBool = Math.random() < 0.5; // 50% chance to be negative
            boolean directional = stock.getVolatility() == Volatility.SLOW_FALL
                    || stock.getVolatility() == Volatility.NORMAL_FALL
                    || stock.getVolatility() == Volatility.SLOW_RISE
                    || stock.getVolatility() == Volatility.NORMAL_RISE;
            if (!directional && randomBool) {
                multiplicativeChange = BigDecimal.ONE.divide(multiplicativeChange, 4, RoundingMode.HALF_UP); // Randomly invert to add unpredictability
            }

            BigDecimal newPrice = currentPrice.multiply(multiplicativeChange);
            stock.addNewSalesPrice(newPrice);
        }

        // Per-stock volatility phase transitions — each stock independently has ~25% chance to shift phase each week
        for (Stock s : stockMap.values()) {
            if (random.nextDouble() < 0.25) {
                s.setVolatility(pickNextVolatility(s.getVolatility()));
            }
        }

        // Spike events — each tier independently fires and applies to one random stock
        List<Stock> allStocks = new ArrayList<>(stockMap.values());
        applySpike(allStocks, 0.10, 10,  75);
        applySpike(allStocks, 0.05, 20, 100);
        applySpike(allStocks, 0.02, 40, 150);
        applySpike(allStocks, 0.01, 50, 200);
    }

    private void applySpike(List<Stock> stocks, double chance, double minPct, double maxPct) {
        if (random.nextDouble() >= chance) return;
        Stock target = stocks.get(random.nextInt(stocks.size()));
        double pct = minPct + random.nextDouble() * (maxPct - minPct);
        BigDecimal factor = BigDecimal.valueOf(1.0 + pct / 100.0).setScale(6, RoundingMode.HALF_UP);
        BigDecimal newPrice;
        if (random.nextBoolean()) {
            newPrice = target.getSalesPrice().multiply(factor);
        } else {
            newPrice = target.getSalesPrice().divide(factor, 6, RoundingMode.HALF_UP);
        }
        target.addNewSalesPrice(newPrice);
    
    }

    private Volatility pickNextVolatility(Volatility current) {
        double[] weights = VOLATILITY_TRANSITIONS[current.ordinal()];
        double total = 0;
        for (double w : weights) total += w;
        double pick = random.nextDouble() * total;
        double cumulative = 0;
        Volatility[] vals = Volatility.values();
        for (int i = 0; i < weights.length; i++) {
            cumulative += weights[i];
            if (pick < cumulative) return vals[i];
        }
        return vals[0];
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
