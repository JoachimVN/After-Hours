package edu.ntnu.idatt2003.g23.io;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import edu.ntnu.idatt2003.g23.model.Stock;

import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
}
