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

    // ─── writeHistory tests ───────────────────────────────────────────────────

    @Test
    @DisplayName("writeHistory throws for null file")
    void writeHistoryNullFileThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> StockCsvExporter.writeHistory(null, List.of()));
    }

    @Test
    @DisplayName("writeHistory throws for null stock collection")
    void writeHistoryNullStocksThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> StockCsvExporter.writeHistory(tempDir.resolve("h.csv"), null));
    }

    @Test
    @DisplayName("writeHistory writes symbol,company,prices semicolon-delimited")
    void writeHistoryWritesCorrectFormat() throws IOException {
        Stock stock = new Stock("AAPL", "Apple Inc.",
                new ArrayList<>(List.of(new BigDecimal("100"), new BigDecimal("110"), new BigDecimal("120"))));
        Path out = tempDir.resolve("history.csv");

        StockCsvExporter.writeHistory(out, List.of(stock));

        List<String> lines = Files.readAllLines(out);
        assertEquals(1, lines.size());
        assertEquals("AAPL,Apple Inc.,100;110;120", lines.get(0));
    }

    @Test
    @DisplayName("writeHistory skips null elements")
    void writeHistorySkipsNullElements() throws IOException {
        List<Stock> stocks = new ArrayList<>();
        stocks.add(null);
        Path out = tempDir.resolve("history_null.csv");

        StockCsvExporter.writeHistory(out, stocks);

        List<String> lines = Files.readAllLines(out);
        assertEquals(0, lines.size());
    }

    @Test
    @DisplayName("writeHistory writes multiple stocks")
    void writeHistoryMultipleStocks() throws IOException {
        Stock aapl = new Stock("AAPL", "Apple Inc.", new ArrayList<>(List.of(new BigDecimal("100"))));
        Stock googl = new Stock("GOOGL", "Google LLC", new ArrayList<>(List.of(new BigDecimal("200"), new BigDecimal("210"))));
        Path out = tempDir.resolve("history_multi.csv");

        StockCsvExporter.writeHistory(out, List.of(aapl, googl));

        List<String> lines = Files.readAllLines(out);
        assertEquals(2, lines.size());
        assertEquals("AAPL,Apple Inc.,100", lines.get(0));
        assertEquals("GOOGL,Google LLC,200;210", lines.get(1));
    }

    // ─── writeCsvRows tests ───────────────────────────────────────────────────

    @Test
    @DisplayName("writeCsvRows throws for null target")
    void writeCsvRowsNullTargetThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> StockCsvExporter.writeCsvRows(null, List.of()));
    }

    @Test
    @DisplayName("writeCsvRows throws for null rows list")
    void writeCsvRowsNullRowsThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> StockCsvExporter.writeCsvRows(tempDir.resolve("rows.csv"), null));
    }

    @Test
    @DisplayName("writeCsvRows writes header and one row")
    void writeCsvRowsWritesHeaderAndRow() throws IOException {
        CsvRow row = new CsvRow(1, "AAPL", "Apple Inc.", "100;110;120", null);
        Path out = tempDir.resolve("rows.csv");

        StockCsvExporter.writeCsvRows(out, List.of(row));

        List<String> lines = Files.readAllLines(out);
        assertEquals(2, lines.size());
        assertEquals("symbol,company,prices", lines.get(0));
        assertTrue(lines.get(1).startsWith("AAPL,Apple Inc.,"));
        assertTrue(lines.get(1).contains("100;110;120"));
    }

    @Test
    @DisplayName("writeCsvRows writes header only for empty row list")
    void writeCsvRowsEmptyList() throws IOException {
        Path out = tempDir.resolve("rows_empty.csv");

        StockCsvExporter.writeCsvRows(out, List.of());

        List<String> lines = Files.readAllLines(out);
        assertEquals(1, lines.size());
        assertEquals("symbol,company,prices", lines.get(0));
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
