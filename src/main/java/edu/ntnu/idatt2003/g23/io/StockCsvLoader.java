package edu.ntnu.idatt2003.g23.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import edu.ntnu.idatt2003.g23.model.Stock;

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

        return parse(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
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

    // ── Lenient parsing (for the in-game CSV editor) ─────────────────────────

    /**
     * Loads stocks from a classpath resource, collecting errors instead of
     * throwing.  Every non-blank, non-comment data line is returned as a
     * {@link CsvRow}; invalid rows carry a non-empty error message.
     *
     * @param resourcePath path in resources, e.g. {@code "data/stocks/sp500_stocks.csv"}
     * @return parse result containing all rows
     */
    public static CsvParseResult loadFromResourceWithErrors(String resourcePath) {
        InputStream inputStream = StockCsvLoader.class.getClassLoader().getResourceAsStream(resourcePath);
        if (inputStream == null) {
            throw new IllegalStateException("Resource not found: " + resourcePath);
        }
        return parseWithErrors(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
    }

    /**
     * Parses stocks from a CSV reader, collecting errors instead of throwing.
     * Every non-blank, non-comment data line is returned as a {@link CsvRow};
     * invalid rows carry a non-empty error message.
     *
     * @param reader source reader
     * @return parse result containing all rows
     */
    public static CsvParseResult parseWithErrors(Reader reader) {
        List<CsvRow> rows = new ArrayList<>();
        int[] lineCounter = {0};

        try (BufferedReader bufferedReader = new BufferedReader(reader)) {
            String line;
            boolean firstLine = true;

            while ((line = bufferedReader.readLine()) != null) {
                lineCounter[0]++;
                String trimmed = line.trim();

                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }

                if (firstLine && trimmed.toLowerCase().startsWith("symbol,")) {
                    firstLine = false;
                    continue;
                }
                firstLine = false;

                rows.add(parseRowLenient(trimmed, lineCounter[0]));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse stock CSV", e);
        }

        return new CsvParseResult(rows);
    }

    /**
     * Converts a valid (error-free) {@link CsvRow} to a {@link Stock}.
     * The row must have passed {@link #validateRow(CsvRow)} without errors
     * before calling this method.
     *
     * @param row a row with no validation error
     * @return the corresponding Stock
     */
    public static Stock rowToStock(CsvRow row) {
        String[] rawPrices = row.getPrices().split(";");
        List<BigDecimal> prices = new ArrayList<>();
        for (String p : rawPrices) {
            String trimmed = p.trim();
            if (!trimmed.isEmpty()) {
                prices.add(new BigDecimal(trimmed));
            }
        }
        return new Stock(row.getSymbol().trim(), row.getCompany().trim(), prices);
    }

    /**
     * Validates a {@link CsvRow} in place: re-evaluates the current field
     * values and updates the row's {@code errorMessage} property.
     *
     * @param row the row to validate
     */
    public static void validateRow(CsvRow row) {
        String symbol  = row.getSymbol().trim();
        String company = row.getCompany().trim();
        String prices  = row.getPrices().trim();

        if (symbol.isEmpty()) {
            row.setErrorMessage("Symbol must not be empty");
            return;
        }
        if (!symbol.matches("[A-Z]+(\\.[A-Z]+)*")) {
            row.setErrorMessage("Symbol must be uppercase letters only (e.g. AAPL or BRK.A): \"" + symbol + "\"");
            return;
        }
        if (company.isEmpty()) {
            row.setErrorMessage("Company must not be empty");
            return;
        }
        if (prices.isEmpty()) {
            row.setErrorMessage("Stock must have at least one price");
            return;
        }

        String[] rawPrices = prices.split(";");
        boolean hasPrices = false;
        for (String raw : rawPrices) {
            String p = raw.trim();
            if (!p.isEmpty()) {
                try {
                    BigDecimal val = new BigDecimal(p);
                    if (val.compareTo(BigDecimal.ZERO) <= 0) {
                        row.setErrorMessage("Price must be positive: \"" + p + "\"");
                        return;
                    }
                    hasPrices = true;
                } catch (NumberFormatException e) {
                    row.setErrorMessage("Invalid price value: \"" + p + "\"");
                    return;
                }
            }
        }

        if (!hasPrices) {
            row.setErrorMessage("Stock must have at least one price");
            return;
        }

        row.setErrorMessage("");
    }

    /** Parses one line leniently, never throwing — errors go into the returned row. */
    private static CsvRow parseRowLenient(String line, int lineNumber) {
        String[] parts = line.split(",", 3);
        String symbol  = parts.length > 0 ? parts[0].trim() : "";
        String company = parts.length > 1 ? parts[1].trim() : "";
        String prices  = parts.length > 2 ? parts[2].trim() : "";

        if (parts.length != 3) {
            return new CsvRow(lineNumber, symbol, company, prices,
                    "Expected 3 columns (symbol,company,prices) but found " + parts.length);
        }
        if (symbol.isEmpty()) {
            return new CsvRow(lineNumber, symbol, company, prices, "Symbol must not be empty");
        }
        if (!symbol.matches("[A-Z]+(\\.[A-Z]+)*")) {
            return new CsvRow(lineNumber, symbol, company, prices,
                    "Symbol must be uppercase letters only (e.g. AAPL or BRK.A): \"" + symbol + "\"");
        }
        if (company.isEmpty()) {
            return new CsvRow(lineNumber, symbol, company, prices, "Company must not be empty");
        }

        String[] rawPrices = prices.split(";");
        boolean hasPrices = false;
        for (String raw : rawPrices) {
            String p = raw.trim();
            if (!p.isEmpty()) {
                try {
                    BigDecimal val = new BigDecimal(p);
                    if (val.compareTo(BigDecimal.ZERO) <= 0) {
                        return new CsvRow(lineNumber, symbol, company, prices,
                                "Price must be positive: \"" + p + "\"");
                    }
                    hasPrices = true;
                } catch (NumberFormatException e) {
                    return new CsvRow(lineNumber, symbol, company, prices,
                            "Invalid price value: \"" + p + "\"");
                }
            }
        }

        if (!hasPrices) {
            return new CsvRow(lineNumber, symbol, company, prices,
                    "Stock must have at least one price");
        }

        return new CsvRow(lineNumber, symbol, company, prices, "");
    }
}

// CoPilot helped with the implementation of this class