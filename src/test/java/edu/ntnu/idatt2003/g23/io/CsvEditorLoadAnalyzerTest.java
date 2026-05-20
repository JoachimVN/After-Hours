package edu.ntnu.idatt2003.g23.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class CsvEditorLoadAnalyzerTest {

  @Test
  void analyze_countsOnlyDisplayRowsAndPriceTokens() {
    CsvRow visibleWithPrices = new CsvRow(1, "AAPL", "Apple", "100; 101 ;102", "");
    CsvRow blankNonDisplay = new CsvRow(2, "", "", "", "");
    CsvRow visibleByErrorOnly = new CsvRow(3, "", "", "", "Malformed row");
    CsvRow visibleWithSpacedPrices = new CsvRow(4, "MSFT", "Microsoft", " 200 ;   201;   ", "");

    CsvParseResult result = new CsvParseResult(
        List.of(visibleWithPrices, blankNonDisplay, visibleByErrorOnly, visibleWithSpacedPrices));

    CsvEditorLoadAnalyzer.LoadStats stats = CsvEditorLoadAnalyzer.analyze(result);

    assertEquals(3, stats.rowCount());
    assertEquals(5, stats.pricePointCount());
    assertEquals("100; 101 ;102".length() + " 200 ;   201;   ".length(), stats.priceCharCount());
  }

  @Test
  void shouldWarn_triggersWhenAnyThresholdIsExceeded() {
    CsvEditorLoadAnalyzer.LoadStats overRows =
        new CsvEditorLoadAnalyzer.LoadStats(CsvEditorLoadAnalyzer.ROW_WARN_THRESHOLD + 1, 0, 0);
    CsvEditorLoadAnalyzer.LoadStats overPoints =
        new CsvEditorLoadAnalyzer.LoadStats(0,
            CsvEditorLoadAnalyzer.PRICE_POINTS_WARN_THRESHOLD + 1, 0);
    CsvEditorLoadAnalyzer.LoadStats overChars =
        new CsvEditorLoadAnalyzer.LoadStats(0, 0,
            CsvEditorLoadAnalyzer.PRICE_CHARS_WARN_THRESHOLD + 1);

    assertTrue(CsvEditorLoadAnalyzer.shouldWarn(overRows));
    assertTrue(CsvEditorLoadAnalyzer.shouldWarn(overPoints));
    assertTrue(CsvEditorLoadAnalyzer.shouldWarn(overChars));
  }

  @Test
  void shouldWarn_isFalseAtOrBelowThresholds() {
    CsvEditorLoadAnalyzer.LoadStats atThresholds =
        new CsvEditorLoadAnalyzer.LoadStats(
            CsvEditorLoadAnalyzer.ROW_WARN_THRESHOLD,
            CsvEditorLoadAnalyzer.PRICE_POINTS_WARN_THRESHOLD,
            CsvEditorLoadAnalyzer.PRICE_CHARS_WARN_THRESHOLD);

    assertFalse(CsvEditorLoadAnalyzer.shouldWarn(atThresholds));
  }
}
