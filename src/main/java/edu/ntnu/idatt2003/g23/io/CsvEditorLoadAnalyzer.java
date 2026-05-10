package edu.ntnu.idatt2003.g23.io;

/**
 * Analyses a {@link CsvParseResult} to decide whether opening it in the CSV editor is likely to
 * cause performance issues, and produces a summary of its size for use in warnings.
 */
public final class CsvEditorLoadAnalyzer {

  public static final int ROW_WARN_THRESHOLD = 1_000;
  public static final long PRICE_POINTS_WARN_THRESHOLD = 500_000;
  public static final long PRICE_CHARS_WARN_THRESHOLD = 5_000_000;

  /**
   * Summary of the data volume in a CSV parse result as seen by the editor.
   */
  public record LoadStats(int rowCount, long pricePointCount, long priceCharCount) {}

  /**
   * Computes load statistics for the rows that the CSV editor will actually render.
   */
  public static LoadStats analyze(CsvParseResult result) {
    int displayRowCount = 0;
    long pricePointCount = 0;
    long priceCharCount = 0;

    for (CsvRow row : result.getRows()) {
      if (!isDisplayRow(row)) {
        continue;
      }
      displayRowCount++;
      String prices = row.getPrices();
      if (prices == null || prices.isBlank()) {
        continue;
      }
      priceCharCount += prices.length();
      boolean inToken = false;
      for (int i = 0; i < prices.length(); i++) {
        char ch = prices.charAt(i);
        if (ch == ';') {
          inToken = false;
        } else if (!Character.isWhitespace(ch) && !inToken) {
          pricePointCount++;
          inToken = true;
        }
      }
    }

    return new LoadStats(displayRowCount, pricePointCount, priceCharCount);
  }

  /**
   * Returns {@code true} when the load stats exceed any warning threshold.
   */
  public static boolean shouldWarn(LoadStats stats) {
    return stats.rowCount() > ROW_WARN_THRESHOLD
        || stats.pricePointCount() > PRICE_POINTS_WARN_THRESHOLD
        || stats.priceCharCount() > PRICE_CHARS_WARN_THRESHOLD;
  }

  private static boolean isDisplayRow(CsvRow row) {
    return row != null
        && (!row.getSymbol().isBlank()
        || !row.getCompany().isBlank()
        || !row.getPrices().isBlank()
        || !row.getErrorMessage().isBlank());
  }

  private CsvEditorLoadAnalyzer() {}
}
