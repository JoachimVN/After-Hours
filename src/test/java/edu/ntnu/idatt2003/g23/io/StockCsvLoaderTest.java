package edu.ntnu.idatt2003.g23.io;

import org.junit.jupiter.api.DisplayName;
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
        List<Stock> stocks = StockCsvLoader.loadFromResource("data/stocks/sp500_stocks.csv");

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
        assertDoesNotThrow(() -> { constructor.newInstance(); });
    }
}
