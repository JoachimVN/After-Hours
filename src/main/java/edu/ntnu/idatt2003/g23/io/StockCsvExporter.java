package edu.ntnu.idatt2003.g23.io;

import edu.ntnu.idatt2003.g23.model.Stock;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

public final class StockCsvExporter {

    private StockCsvExporter() {
        // utility
    }

    /**
     * Writes current stock prices (latest sales price) to CSV.
     * Columns: symbol,company,price
     */
    public static void writeCurrentPrices(Path file, Collection<Stock> stocks) throws IOException {
        if (file == null) throw new IllegalArgumentException("file cannot be null");
        if (stocks == null) throw new IllegalArgumentException("stocks cannot be null");

        Files.createDirectories(file.getParent());

        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write("symbol,company,price");
            writer.newLine();

            for (Stock stock : stocks) {
                if (stock == null) continue;

                String symbol = csv(stock.getSymbol());
                String company = csv(stock.getCompany());

                BigDecimal price;
                try {
                    price = stock.getSalesPrice();
                } catch (RuntimeException ex) {
                    // If a stock has no prices, skip it (or choose to write empty price instead).
                    continue;
                }

                writer.write(symbol);
                writer.write(',');
                writer.write(company);
                writer.write(',');
                writer.write(price.toPlainString());
                writer.newLine();
            }
        }
    }

    /**
     * Writes full price history to CSV in the semicolon-delimited format:
     * {@code SYMBOL,Company Name,price1;price2;price3;...}
     * This format is compatible with {@link StockCsvLoader}.
     */
    public static void writeHistory(Path file, Collection<Stock> stocks) throws IOException {
        if (file == null) throw new IllegalArgumentException("file cannot be null");
        if (stocks == null) throw new IllegalArgumentException("stocks cannot be null");

        Files.createDirectories(file.getParent());

        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            for (Stock stock : stocks) {
                if (stock == null) continue;
                List<BigDecimal> prices = stock.getHistoricalPrices();
                if (prices == null || prices.isEmpty()) continue;

                writer.write(csv(stock.getSymbol()));
                writer.write(',');
                writer.write(csv(stock.getCompany()));
                writer.write(',');
                StringBuilder priceStr = new StringBuilder();
                for (int i = 0; i < prices.size(); i++) {
                    if (i > 0) priceStr.append(';');
                    priceStr.append(prices.get(i).toPlainString());
                }
                writer.write(priceStr.toString());
                writer.newLine();
            }
        }
    }

    private static String csv(String value) {
        if (value == null) return "";
        boolean mustQuote = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r");
        String escaped = value.replace("\"", "\"\"");
        return mustQuote ? "\"" + escaped + "\"" : escaped;
    }
}