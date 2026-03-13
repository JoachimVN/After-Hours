package edu.ntnu.idatt2003.g23.model;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

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

    
    
}
