package edu.ntnu.idatt2003.g23.io;

import edu.ntnu.idatt2003.g23.AppConfig;
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
    InputStream inputStream =
        StockCsvLoader.class.getClassLoader().getResourceAsStream(resourcePath);
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
    String rawPrices = parts[2].trim();
    if (AppConfig.PERFORMANCE_MODE.get()) {
      return new Stock(symbol, company, () -> parsePriceList(rawPrices, true));
    }
    return new Stock(symbol, company, parsePriceList(rawPrices, false));
  }

  // ── Lenient parsing (for the in-game CSV editor) ─────────────────────────

  /**
   * Loads stocks from a classpath resource, collecting errors instead of
   * throwing.  Every non-blank, non-comment data line is returned as a
   * {@link CsvRow}; invalid rows carry a non-empty error message.
   *
   * @param resourcePath path in resources, e.g. {@code "data/stocks/sp500.csv"}
   * @return parse result containing all rows
   */
  public static CsvParseResult loadFromResourceWithErrors(String resourcePath) {
    InputStream inputStream =
        StockCsvLoader.class.getClassLoader().getResourceAsStream(resourcePath);
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
    String prices = row.getPrices();
    if (AppConfig.PERFORMANCE_MODE.get()) {
      return new Stock(
          row.getSymbol().trim(),
          row.getCompany().trim(),
          () -> parsePriceList(prices, true));
    }
    return new Stock(row.getSymbol().trim(), row.getCompany().trim(), parsePriceList(prices, false));
  }

  /**
   * Converts a list of valid (error-free) {@link CsvRow} objects to a list of {@link Stock}s.
   */
  public static List<Stock> toStocks(List<CsvRow> rows) {
    return rows.stream().map(StockCsvLoader::rowToStock).toList();
  }

  private static List<BigDecimal> parsePriceList(String rawPrices, boolean applyCap) {
    String[] raw = rawPrices.trim().split(";");
    List<BigDecimal> prices = new ArrayList<>();
    for (String token : raw) {
      String trimmed = token.trim();
      if (!trimmed.isEmpty()) {
        prices.add(new BigDecimal(trimmed));
      }
    }

    if (prices.isEmpty()) {
      throw new IllegalArgumentException("Stock must have at least one price");
    }

    if (applyCap) {
      int maxWeeks = Math.max(50, AppConfig.PERFORMANCE_MAX_HISTORY_WEEKS.get());
      if (prices.size() > maxWeeks) {
        return new ArrayList<>(prices.subList(prices.size() - maxWeeks, prices.size()));
      }
    }
    return prices;
  }

  /**
   * Validates a {@link CsvRow} in place: re-evaluates the current field
   * values and updates the row's {@code errorMessage} and {@code errorColumn} properties.
   *
   * @param row the row to validate
   */
  public static void validateRow(CsvRow row) {
    String symbol = row.getSymbol().trim();
    String company = row.getCompany().trim();
    String prices = row.getPrices().trim();

    if (symbol.isEmpty()) {
      setError(row, "symbol", "Symbol must not be empty");
      return;
    }
    if (!symbol.matches("[A-Z]+(\\.[A-Z]+)*")) {
      setError(row, "symbol", buildSymbolError(symbol));
      return;
    }
    if (company.isEmpty()) {
      setError(row, "company", "Company name must not be empty");
      return;
    }
    if (prices.isEmpty()) {
      setError(row, "prices", "No price set — add at least one price (e.g. 214.10)");
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
            setError(row, "prices",
                "Price \"" + p + "\" must be greater than zero");
            return;
          }
          hasPrices = true;
        } catch (NumberFormatException e) {
          setError(row, "prices",
              "\"" + p + "\" isn't a valid price — enter a number like 214.10");
          return;
        }
      }
    }

    if (!hasPrices) {
      setError(row, "prices", "No price set — add at least one price (e.g. 214.10)");
      return;
    }

    clearError(row);
  }

  /**
   * Sets both errorMessage and errorColumn on a row.
   */
  private static void setError(CsvRow row, String column, String message) {
    row.setErrorMessage(message);
    row.setErrorColumn(column);
  }

  /**
   * Clears both errorMessage and errorColumn on a row.
   */
  private static void clearError(CsvRow row) {
    row.setErrorMessage("");
    row.setErrorColumn("");
  }

  /**
   * Builds a simple, user-friendly error message for an invalid symbol.
   */
  private static String buildSymbolError(String symbol) {
    for (int i = 0; i < symbol.length(); i++) {
      char c = symbol.charAt(i);
      if (c != '.' && !Character.isUpperCase(c)) {
        if (Character.isDigit(c)) {
          return "\"" + symbol + "\" can't contain numbers — use letters only (e.g. AAPL)";
        } else if (Character.isLowerCase(c)) {
          return "\"" + symbol + "\" must be uppercase — try \"" + symbol.toUpperCase() + "\"";
        } else {
          return "\"" + symbol +
              "\" can't contain special characters — use letters only (e.g. AAPL)";
        }
      }
    }
    return "\"" + symbol + "\" is not a valid symbol — use uppercase letters only (e.g. AAPL)";
  }

  /**
   * Parses one line leniently, never throwing — errors go into the returned row.
   */
  private static CsvRow parseRowLenient(String line, int lineNumber) {
    String[] parts = line.split(",", 3);
    String symbol = parts[0].trim();
    String company = parts.length > 1 ? parts[1].trim() : "";
    String prices = parts.length > 2 ? parts[2].trim() : "";

    if (parts.length < 3) {
      String missing = parts.length == 1
          ? "missing the company name and price"
          : "missing the price";
      return rowWithError(lineNumber, symbol, company, prices, "",
          "Row is incomplete — " + missing + " (format should be: symbol,company,price)");
    }
    if (symbol.isEmpty()) {
      return rowWithError(lineNumber, symbol, company, prices, "symbol",
          "Symbol must not be empty");
    }
    if (!symbol.matches("[A-Z]+(\\.[A-Z]+)*")) {
      return rowWithError(lineNumber, symbol, company, prices, "symbol",
          buildSymbolError(symbol));
    }
    if (company.isEmpty()) {
      return rowWithError(lineNumber, symbol, company, prices, "company",
          "Company name must not be empty");
    }
    if (prices.isEmpty()) {
      return rowWithError(lineNumber, symbol, company, prices, "prices",
          "No price set — add at least one price (e.g. 214.10)");
    }

    String[] rawPrices = prices.split(";");
    boolean hasPrices = false;
    for (String raw : rawPrices) {
      String p = raw.trim();
      if (!p.isEmpty()) {
        try {
          BigDecimal val = new BigDecimal(p);
          if (val.compareTo(BigDecimal.ZERO) <= 0) {
            return rowWithError(lineNumber, symbol, company, prices, "prices",
                "Price \"" + p + "\" must be greater than zero");
          }
          hasPrices = true;
        } catch (NumberFormatException e) {
          return rowWithError(lineNumber, symbol, company, prices, "prices",
              "\"" + p + "\" isn't a valid price — enter a number like 214.10");
        }
      }
    }

    if (!hasPrices) {
      return rowWithError(lineNumber, symbol, company, prices, "prices",
          "No price set — add at least one price (e.g. 214.10)");
    }

    return new CsvRow(lineNumber, symbol, company, prices, "");
  }

  /**
   * Creates a CsvRow with an error message and the column that caused it.
   */
  private static CsvRow rowWithError(int lineNumber, String symbol, String company,
                                     String prices, String errorColumn, String errorMessage) {
    CsvRow row = new CsvRow(lineNumber, symbol, company, prices, errorMessage);
    row.setErrorColumn(errorColumn);
    return row;
  }
}

// CoPilot helped with the implementation of this class