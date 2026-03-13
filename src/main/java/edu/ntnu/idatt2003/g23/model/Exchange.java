package edu.ntnu.idatt2003.g23.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import edu.ntnu.idatt2003.g23.model.transaction.Purchase;
import edu.ntnu.idatt2003.g23.model.transaction.Sale;
import edu.ntnu.idatt2003.g23.model.transaction.Transaction;

public class Exchange {
    private final String name;
    private int week;
    private Map<String, Stock> stockMap;
    private Random random;

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

    public String getName() {
        return name;
    }

    public int getWeek() {
        return week;
    }

    public boolean hasStock(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("Stock symbol cannot be null or empty");
        }
        return stockMap.containsKey(symbol);
    }

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

    public Transaction sell(Share share, Player player) {
        if (share == null) {
            throw new IllegalArgumentException("Share cannot be null");
        }
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }

        Stock stock = share.getStock();
        BigDecimal currentPrice = stock.getSalesPrice();
        Share sellShare = new Share(stock, share.getQuantity(), currentPrice);
        return new Sale(sellShare, this.week);
    }

    public void advance() {
        this.week++;
        for (Stock stock : stockMap.values()) {
            BigDecimal currentPrice = stock.getSalesPrice();
            double percentageChange = (random.nextDouble() * 20) - 10;  // AI - -10% to +10%
            BigDecimal newPrice = currentPrice.multiply(BigDecimal.valueOf(1 + (percentageChange / 100)));
            stock.addNewSalesPrice(newPrice);
        }
    }
}
