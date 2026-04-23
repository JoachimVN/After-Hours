package edu.ntnu.idatt2003.g23.io;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import edu.ntnu.idatt2003.g23.model.Stock;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


class StockCsvLoaderTest {

  @Test
  @DisplayName("parse creates Stock objects from valid CSV")
  void parseValidCsv() {
    String csv = """
        symbol,company,prices
        AAPL,Apple Inc.,100.50;101.25
        TSLA,Tesla Inc.,200.00
        """;

    List<Stock> stocks = StockCsvLoader.parse(new StringReader(csv));

    assertEquals(2, stocks.size());
    assertEquals("AAPL", stocks.get(0).getSymbol());
    assertEquals("Apple Inc.", stocks.get(0).getCompany());
    assertEquals("101.25", stocks.get(0).getSalesPrice().toPlainString());

    assertEquals("TSLA", stocks.get(1).getSymbol());
    assertEquals("200.00", stocks.get(1).getSalesPrice().toPlainString());
  }

  @Test
  @DisplayName("parse throws for malformed line")
  void parseMalformedLineThrows() {
    String csv = "AAPL,Apple Inc.";

    assertThrows(IllegalArgumentException.class, () -> StockCsvLoader.parse(new StringReader(csv)));
  }

  @Test
  @DisplayName("parse throws when no prices are present")
  void parseNoPriceThrows() {
    String csv = "AAPL,Apple Inc.,   ";

    assertThrows(IllegalArgumentException.class, () -> StockCsvLoader.parse(new StringReader(csv)));
  }

  @Test
  @DisplayName("parse skips blank lines")
  void parseSkipsEmptyLines() {
    String csv = "AAPL,Apple Inc.,100\n\nTSLA,Tesla Inc.,200";

    List<Stock> stocks = StockCsvLoader.parse(new StringReader(csv));

    assertEquals(2, stocks.size());
  }

  @Test
  @DisplayName("parse skips comment lines starting with #")
  void parseSkipsCommentLines() {
    String csv = "# this is a comment\nAAPL,Apple Inc.,100";

    List<Stock> stocks = StockCsvLoader.parse(new StringReader(csv));

    assertEquals(1, stocks.size());
    assertEquals("AAPL", stocks.get(0).getSymbol());
  }

  @Test
  @DisplayName("parse reads CSV without a header row")
  void parseNoHeader() {
    String csv = "AAPL,Apple Inc.,100";

    List<Stock> stocks = StockCsvLoader.parse(new StringReader(csv));

    assertEquals(1, stocks.size());
    assertEquals("AAPL", stocks.get(0).getSymbol());
  }

  @Test
  @DisplayName("parse wraps reader IOException as IllegalStateException")
  void parseIOExceptionThrows() {
    Reader broken = new Reader() {
      @Override
      public int read(char[] buf, int off, int len) throws IOException {
        throw new IOException("simulated read failure");
      }

      @Override
      public void close() {
      }
    };

    assertThrows(IllegalStateException.class, () -> StockCsvLoader.parse(broken));
  }

  @Test
  @DisplayName("loadFromResource returns stocks for the bundled CSV")
  void loadFromResourceValidPath() {
    List<Stock> stocks = StockCsvLoader.loadFromResource("data/stocks/sp500.csv");

    assertFalse(stocks.isEmpty());
  }

  @Test
  @DisplayName("loadFromResource throws for a nonexistent path")
  void loadFromResourceMissingPathThrows() {
    assertThrows(IllegalStateException.class,
        () -> StockCsvLoader.loadFromResource("nonexistent/missing.csv"));
  }

  @Test
  @DisplayName("Private constructor does not throw")
  void testPrivateConstructor() throws Exception {
    var constructor = StockCsvLoader.class.getDeclaredConstructor();
    constructor.setAccessible(true);
    assertDoesNotThrow(() -> {
      constructor.newInstance();
    });
  }

  // ── parseWithErrors ───────────────────────────────────────────────────────

  @Nested
  @DisplayName("parseWithErrors tests")
  class ParseWithErrorsTests {

    @Test
    @DisplayName("Valid CSV produces rows with no errors")
    void testValidCsv() {
      String csv = "symbol,company,prices\nAAPL,Apple Inc.,150.00\nTSLA,Tesla Inc.,200.00;210.00";
      CsvParseResult result = StockCsvLoader.parseWithErrors(new StringReader(csv));
      assertEquals(2, result.getRows().size());
      assertFalse(result.hasErrors());
    }

    @Test
    @DisplayName("Skips blank and comment lines")
    void testSkipsBlankAndComments() {
      String csv = "# comment\n\nAAPL,Apple Inc.,150.00";
      CsvParseResult result = StockCsvLoader.parseWithErrors(new StringReader(csv));
      assertEquals(1, result.getRows().size());
    }

    @Test
    @DisplayName("Row with only 1 column gets error (missing company and price)")
    void testOneColumnRow() {
      CsvParseResult result = StockCsvLoader.parseWithErrors(new StringReader("AAPL"));
      assertEquals(1, result.getRows().size());
      assertTrue(result.getRows().get(0).hasError());
      assertTrue(
          result.getRows().get(0).getErrorMessage().contains("missing the company name and price"));
    }

    @Test
    @DisplayName("Row with only 2 columns gets error (missing price)")
    void testTwoColumnRow() {
      CsvParseResult result = StockCsvLoader.parseWithErrors(new StringReader("AAPL,Apple Inc."));
      assertTrue(result.getRows().get(0).hasError());
      assertTrue(result.getRows().get(0).getErrorMessage().contains("missing the price"));
    }

    @Test
    @DisplayName("Empty symbol gets error")
    void testEmptySymbol() {
      CsvParseResult result =
          StockCsvLoader.parseWithErrors(new StringReader(",Apple Inc.,150.00"));
      assertTrue(result.getRows().get(0).hasError());
      assertEquals("symbol", result.getRows().get(0).getErrorColumn());
    }

    @Test
    @DisplayName("Lowercase symbol gets descriptive error")
    void testLowercaseSymbol() {
      CsvParseResult result =
          StockCsvLoader.parseWithErrors(new StringReader("aapl,Apple Inc.,150.00"));
      CsvRow row = result.getRows().get(0);
      assertTrue(row.hasError());
      assertTrue(row.getErrorMessage().contains("uppercase"));
    }

    @Test
    @DisplayName("Symbol with digit gets descriptive error")
    void testDigitInSymbol() {
      CsvParseResult result =
          StockCsvLoader.parseWithErrors(new StringReader("A1PL,Apple Inc.,150.00"));
      CsvRow row = result.getRows().get(0);
      assertTrue(row.hasError());
      assertTrue(row.getErrorMessage().contains("numbers"));
    }

    @Test
    @DisplayName("Symbol with special character gets descriptive error")
    void testSpecialCharInSymbol() {
      CsvParseResult result =
          StockCsvLoader.parseWithErrors(new StringReader("A@PL,Apple Inc.,150.00"));
      CsvRow row = result.getRows().get(0);
      assertTrue(row.hasError());
      assertTrue(row.getErrorMessage().contains("special characters"));
    }

    @Test
    @DisplayName("Symbol failing regex but no digit/lower/special falls back to generic error")
    void testSymbolGenericError() {
      // Ends with dot — passes char loop (dot is skipped), fails regex
      CsvParseResult result =
          StockCsvLoader.parseWithErrors(new StringReader("AAPL.,Apple Inc.,150.00"));
      CsvRow row = result.getRows().get(0);
      assertTrue(row.hasError());
      assertTrue(row.getErrorMessage().contains("valid symbol"));
    }

    @Test
    @DisplayName("Empty company gets error")
    void testEmptyCompany() {
      CsvParseResult result = StockCsvLoader.parseWithErrors(new StringReader("AAPL,,150.00"));
      CsvRow row = result.getRows().get(0);
      assertTrue(row.hasError());
      assertEquals("company", row.getErrorColumn());
    }

    @Test
    @DisplayName("Empty prices gets error")
    void testEmptyPrices() {
      CsvParseResult result = StockCsvLoader.parseWithErrors(new StringReader("AAPL,Apple Inc.,"));
      CsvRow row = result.getRows().get(0);
      assertTrue(row.hasError());
      assertEquals("prices", row.getErrorColumn());
    }

    @Test
    @DisplayName("Prices with only whitespace segments gets error")
    void testWhitespaceOnlyPrices() {
      CsvParseResult result =
          StockCsvLoader.parseWithErrors(new StringReader("AAPL,Apple Inc.,   ;   "));
      CsvRow row = result.getRows().get(0);
      assertTrue(row.hasError());
      assertEquals("prices", row.getErrorColumn());
    }

    @Test
    @DisplayName("Zero price gets error")
    void testZeroPrice() {
      CsvParseResult result =
          StockCsvLoader.parseWithErrors(new StringReader("AAPL,Apple Inc.,0.00"));
      CsvRow row = result.getRows().get(0);
      assertTrue(row.hasError());
      assertTrue(row.getErrorMessage().contains("greater than zero"));
    }

    @Test
    @DisplayName("Negative price gets error")
    void testNegativePrice() {
      CsvParseResult result =
          StockCsvLoader.parseWithErrors(new StringReader("AAPL,Apple Inc.,-5.00"));
      CsvRow row = result.getRows().get(0);
      assertTrue(row.hasError());
      assertTrue(row.getErrorMessage().contains("greater than zero"));
    }

    @Test
    @DisplayName("Non-numeric price gets error")
    void testNonNumericPrice() {
      CsvParseResult result =
          StockCsvLoader.parseWithErrors(new StringReader("AAPL,Apple Inc.,abc"));
      CsvRow row = result.getRows().get(0);
      assertTrue(row.hasError());
      assertTrue(row.getErrorMessage().contains("valid price"));
    }

    @Test
    @DisplayName("Empty segments between semicolons are skipped (row is valid)")
    void testEmptySegmentsBetweenSemicolons() {
      CsvParseResult result =
          StockCsvLoader.parseWithErrors(new StringReader("AAPL,Apple Inc.,150.00;;200.00"));
      CsvRow row = result.getRows().get(0);
      assertFalse(row.hasError());
    }

    @Test
    @DisplayName("IOException is wrapped as IllegalStateException")
    void testIOExceptionWrapped() {
      Reader broken = new Reader() {
        @Override
        public int read(char[] buf, int off, int len) throws IOException {
          throw new IOException("fail");
        }

        @Override
        public void close() {
        }
      };
      assertThrows(IllegalStateException.class, () -> StockCsvLoader.parseWithErrors(broken));
    }
  }

  // ── loadFromResourceWithErrors ────────────────────────────────────────────

  @Nested
  @DisplayName("loadFromResourceWithErrors tests")
  class LoadFromResourceWithErrorsTests {

    @Test
    @DisplayName("Returns rows for a bundled CSV")
    void testValidResource() {
      CsvParseResult result = StockCsvLoader.loadFromResourceWithErrors("data/stocks/sp500.csv");
      assertFalse(result.getRows().isEmpty());
    }

    @Test
    @DisplayName("Throws for a nonexistent resource")
    void testMissingResourceThrows() {
      assertThrows(IllegalStateException.class,
          () -> StockCsvLoader.loadFromResourceWithErrors("nonexistent/missing.csv"));
    }
  }

  // ── rowToStock ────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("rowToStock tests")
  class RowToStockTests {

    @Test
    @DisplayName("Converts a valid CsvRow to a Stock correctly")
    void testConvertsRow() {
      CsvRow row = new CsvRow(1, "AAPL", "Apple Inc.", "150.00;160.00", "");
      Stock stock = StockCsvLoader.rowToStock(row);
      assertEquals("AAPL", stock.getSymbol());
      assertEquals("Apple Inc.", stock.getCompany());
      assertEquals(2, stock.getHistoricalPrices().size());
      assertEquals(0, stock.getSalesPrice().compareTo(new java.math.BigDecimal("160.00")));
    }

    @Test
    @DisplayName("Skips empty segments between semicolons")
    void testConvertsRowWithEmptySegments() {
      CsvRow row = new CsvRow(1, "AAPL", "Apple Inc.", "150.00;;160.00", "");
      Stock stock = StockCsvLoader.rowToStock(row);
      assertEquals(2, stock.getHistoricalPrices().size());
    }
  }

  // ── validateRow ───────────────────────────────────────────────────────────

  @Nested
  @DisplayName("validateRow tests")
  class ValidateRowTests {

    private CsvRow row(String symbol, String company, String prices) {
      return new CsvRow(1, symbol, company, prices, "");
    }

    @Test
    @DisplayName("Valid row clears any previous error")
    void testValidRow() {
      CsvRow r = new CsvRow(1, "AAPL", "Apple Inc.", "150.00", "old error");
      StockCsvLoader.validateRow(r);
      assertFalse(r.hasError());
      assertEquals("", r.getErrorColumn());
    }

    @Test
    @DisplayName("Empty symbol sets error on symbol column")
    void testEmptySymbol() {
      CsvRow r = row("", "Apple Inc.", "150.00");
      StockCsvLoader.validateRow(r);
      assertTrue(r.hasError());
      assertEquals("symbol", r.getErrorColumn());
    }

    @Test
    @DisplayName("Invalid symbol (lowercase) sets error on symbol column")
    void testLowercaseSymbol() {
      CsvRow r = row("aapl", "Apple Inc.", "150.00");
      StockCsvLoader.validateRow(r);
      assertTrue(r.hasError());
      assertEquals("symbol", r.getErrorColumn());
      assertTrue(r.getErrorMessage().contains("uppercase"));
    }

    @Test
    @DisplayName("Invalid symbol (digit) sets error on symbol column")
    void testDigitSymbol() {
      CsvRow r = row("A1PL", "Apple Inc.", "150.00");
      StockCsvLoader.validateRow(r);
      assertTrue(r.hasError());
      assertTrue(r.getErrorMessage().contains("numbers"));
    }

    @Test
    @DisplayName("Invalid symbol (special char) sets error on symbol column")
    void testSpecialCharSymbol() {
      CsvRow r = row("A@PL", "Apple Inc.", "150.00");
      StockCsvLoader.validateRow(r);
      assertTrue(r.hasError());
      assertTrue(r.getErrorMessage().contains("special characters"));
    }

    @Test
    @DisplayName("Invalid symbol falling back to generic error")
    void testSymbolGenericError() {
      CsvRow r = row("AAPL.", "Apple Inc.", "150.00");
      StockCsvLoader.validateRow(r);
      assertTrue(r.hasError());
      assertTrue(r.getErrorMessage().contains("valid symbol"));
    }

    @Test
    @DisplayName("Empty company sets error on company column")
    void testEmptyCompany() {
      CsvRow r = row("AAPL", "", "150.00");
      StockCsvLoader.validateRow(r);
      assertTrue(r.hasError());
      assertEquals("company", r.getErrorColumn());
    }

    @Test
    @DisplayName("Empty prices sets error on prices column")
    void testEmptyPrices() {
      CsvRow r = row("AAPL", "Apple Inc.", "");
      StockCsvLoader.validateRow(r);
      assertTrue(r.hasError());
      assertEquals("prices", r.getErrorColumn());
    }

    @Test
    @DisplayName("Prices with only whitespace segments sets error")
    void testWhitespaceOnlyPrices() {
      CsvRow r = row("AAPL", "Apple Inc.", "   ;   ");
      StockCsvLoader.validateRow(r);
      assertTrue(r.hasError());
      assertEquals("prices", r.getErrorColumn());
    }

    @Test
    @DisplayName("Zero price sets error")
    void testZeroPrice() {
      CsvRow r = row("AAPL", "Apple Inc.", "0.00");
      StockCsvLoader.validateRow(r);
      assertTrue(r.hasError());
      assertTrue(r.getErrorMessage().contains("greater than zero"));
    }

    @Test
    @DisplayName("Non-numeric price sets error")
    void testNonNumericPrice() {
      CsvRow r = row("AAPL", "Apple Inc.", "bad");
      StockCsvLoader.validateRow(r);
      assertTrue(r.hasError());
      assertTrue(r.getErrorMessage().contains("valid price"));
    }

    @Test
    @DisplayName("Empty segments between semicolons are skipped (valid row)")
    void testEmptySegmentsBetweenSemicolons() {
      CsvRow r = row("AAPL", "Apple Inc.", "150.00;;160.00");
      StockCsvLoader.validateRow(r);
      assertFalse(r.hasError());
    }
  }
}
