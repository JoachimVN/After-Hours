package edu.ntnu.idatt2003.g23.io;

import static org.junit.jupiter.api.Assertions.*;

import edu.ntnu.idatt2003.g23.model.Stock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

class StockCsvExporterTest {

    @TempDir
    Path tempDir;

    // ─── Guard-clause tests ───────────────────────────────────────────────────

    @Test
    @DisplayName("writeCurrentPrices throws for null file")
    void nullFileThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> StockCsvExporter.writeCurrentPrices(null, List.of()));
    }

    @Test
    @DisplayName("writeCurrentPrices throws for null stock collection")
    void nullStocksThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> StockCsvExporter.writeCurrentPrices(tempDir.resolve("out.csv"), null));
    }

    // ─── Happy-path test ──────────────────────────────────────────────────────

    @Test
    @DisplayName("writeCurrentPrices writes header and one stock row")
    void writesSingleStock() throws IOException {
        Stock stock = new Stock("AAPL", "Apple Inc.", List.of(new BigDecimal("150.25")));
        Path out = tempDir.resolve("out.csv");

        StockCsvExporter.writeCurrentPrices(out, List.of(stock));

        List<String> lines = Files.readAllLines(out);
        assertEquals(2, lines.size());
        assertEquals("symbol,company,price", lines.get(0));
        assertEquals("AAPL,Apple Inc.,150.25", lines.get(1));
    }

    // ─── Null-element skipping ────────────────────────────────────────────────

    @Test
    @DisplayName("writeCurrentPrices skips null elements in the collection")
    void nullElementIsSkipped() throws IOException {
        List<Stock> stocks = new ArrayList<>();
        stocks.add(null);
        Path out = tempDir.resolve("out.csv");

        StockCsvExporter.writeCurrentPrices(out, stocks);

        List<String> lines = Files.readAllLines(out);
        assertEquals(1, lines.size(), "Only the header row should be written");
    }

    // ─── getSalesPrice() failure skipping (reflection) ────────────────────────

    @Test
    @DisplayName("writeCurrentPrices skips a stock whose getSalesPrice() throws")
    void stockWithNoPricesIsSkipped() throws Exception {
        // Construct a valid stock, then empty its internal price list via reflection
        // so that getSalesPrice() throws IndexOutOfBoundsException (a RuntimeException).
        ArrayList<BigDecimal> mutablePrices = new ArrayList<>(List.of(new BigDecimal("100")));
        Stock stock = new Stock("AAPL", "Apple Inc.", mutablePrices);

        Field pricesField = Stock.class.getDeclaredField("prices");
        pricesField.setAccessible(true);
        ((List<?>) pricesField.get(stock)).clear();

        Path out = tempDir.resolve("out.csv");
        StockCsvExporter.writeCurrentPrices(out, List.of(stock));

        List<String> lines = Files.readAllLines(out);
        assertEquals(1, lines.size(), "Only the header row should be written");
    }

    // ─── csv() quoting logic (via reflection) ────────────────────────────────

    private String invokeCsv(String value) throws Exception {
        Method csv = StockCsvExporter.class.getDeclaredMethod("csv", String.class);
        csv.setAccessible(true);
        return (String) csv.invoke(null, value);
    }

    @Test
    @DisplayName("csv(null) returns empty string")
    void csvNullReturnsEmpty() throws Exception {
        assertEquals("", invokeCsv(null));
    }

    @Test
    @DisplayName("csv() quotes a value containing a comma")
    void csvCommaIsQuoted() throws Exception {
        assertEquals("\"Apple, Inc.\"", invokeCsv("Apple, Inc."));
    }

    @Test
    @DisplayName("csv() escapes and quotes a value containing a double-quote")
    void csvQuoteIsEscaped() throws Exception {
        assertEquals("\"Apple \"\"Inc.\"\"\"", invokeCsv("Apple \"Inc.\""));
    }

    @Test
    @DisplayName("csv() quotes a value containing a newline")
    void csvNewlineIsQuoted() throws Exception {
        assertEquals("\"Apple\nInc.\"", invokeCsv("Apple\nInc."));
    }

    @Test
    @DisplayName("csv() quotes a value containing a carriage return")
    void csvCarriageReturnIsQuoted() throws Exception {
        assertEquals("\"Apple\rInc.\"", invokeCsv("Apple\rInc."));
    }

    @Test
    @DisplayName("csv() returns plain value when no quoting is needed")
    void csvPlainValue() throws Exception {
        assertEquals("Apple Inc.", invokeCsv("Apple Inc."));
    }

    // ─── Private constructor ──────────────────────────────────────────────────

    @Test
    @DisplayName("Private constructor does not throw")
    void testPrivateConstructor() throws Exception {
        var constructor = StockCsvExporter.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertDoesNotThrow(() -> { constructor.newInstance(); });
    }
}
