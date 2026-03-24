package edu.ntnu.idatt2003.g23.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

// NOTE: Can currently handle multiple stock prices (logs), could be useful later

/**
 * Utility class for creating {@link Stock} objects from CSV data.
 *
 * <p>CSV format:</p>
 * <pre>
 * symbol,company,prices
 * AAPL,Apple Inc.,214.10
 * TSLA,Tesla Inc.,176.20;179.10
 * </pre>
 *
 * <p>The first line is treated as a header when it starts with {@code symbol,}.</p>
 */
public final class StockCsvLoader {

    private StockCsvLoader() {
        // Utility class
    }

    /**
     * Loads stocks from a classpath resource.
     *
     * @param resourcePath path in resources, e.g. {@code "stocks.csv"}
     * @return list of parsed stocks
     */
    public static List<Stock> loadFromResource(String resourcePath) {
        InputStream inputStream = StockCsvLoader.class.getClassLoader().getResourceAsStream(resourcePath);
        if (inputStream == null) {
            throw new IllegalStateException("Resource not found: " + resourcePath);
        }

        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            return parse(reader);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read stock resource: " + resourcePath, e);
        }
    }

    /**
     * Parses stocks from a CSV reader.
     *
     * @param reader source reader
     * @return list of parsed stocks
     */
    public static List<Stock> parse(Reader reader) {
        List<Stock> stocks = new ArrayList<>();

        try (BufferedReader bufferedReader = new BufferedReader(reader)) {
            String line;
            boolean firstLine = true;

            while ((line = bufferedReader.readLine()) != null) {
                String trimmed = line.trim();

                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }

                if (firstLine && trimmed.toLowerCase().startsWith("symbol,")) {
                    firstLine = false;
                    continue;
                }
                firstLine = false;

                stocks.add(parseStockLine(trimmed));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse stock CSV", e);
        }

        return stocks;
    }

    private static Stock parseStockLine(String line) {
        String[] parts = line.split(",", 3);
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid CSV line (expected 3 columns): " + line);
        }

        String symbol = parts[0].trim();
        String company = parts[1].trim();
        String[] rawPrices = parts[2].trim().split(";");

        List<BigDecimal> prices = new ArrayList<>();
        for (String rawPrice : rawPrices) {
            String price = rawPrice.trim();
            if (!price.isEmpty()) {
                prices.add(new BigDecimal(price));
            }
        }

        if (prices.isEmpty()) {
            throw new IllegalArgumentException("Stock must have at least one price: " + line);
        }

        return new Stock(symbol, company, prices);
    }
}

// CoPilot helped with the implementation of this class