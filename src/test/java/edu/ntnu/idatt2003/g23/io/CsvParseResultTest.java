package edu.ntnu.idatt2003.g23.io;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvParseResultTest {

    private CsvRow validRow() {
        return new CsvRow(1, "AAPL", "Apple Inc.", "150.00", "");
    }

    private CsvRow errorRow() {
        return new CsvRow(2, "", "Apple Inc.", "150.00", "Symbol must not be empty");
    }

    @Test
    @DisplayName("getRows returns all rows in source order")
    void testGetRows() {
        CsvRow r1 = validRow();
        CsvRow r2 = errorRow();
        CsvParseResult result = new CsvParseResult(List.of(r1, r2));
        assertEquals(2, result.getRows().size());
    }

    @Nested
    @DisplayName("hasErrors")
    class HasErrorsTests {

        @Test
        @DisplayName("Returns false when all rows are valid")
        void testNoErrors() {
            CsvParseResult result = new CsvParseResult(List.of(validRow(), validRow()));
            assertFalse(result.hasErrors());
        }

        @Test
        @DisplayName("Returns false for empty result")
        void testEmpty() {
            CsvParseResult result = new CsvParseResult(List.of());
            assertFalse(result.hasErrors());
        }

        @Test
        @DisplayName("Returns true when at least one row has an error")
        void testWithError() {
            CsvParseResult result = new CsvParseResult(List.of(validRow(), errorRow()));
            assertTrue(result.hasErrors());
        }
    }

    @Nested
    @DisplayName("errorCount")
    class ErrorCountTests {

        @Test
        @DisplayName("Returns 0 when no errors")
        void testZeroErrors() {
            CsvParseResult result = new CsvParseResult(List.of(validRow()));
            assertEquals(0, result.errorCount());
        }

        @Test
        @DisplayName("Returns 1 when one error row")
        void testOneError() {
            CsvParseResult result = new CsvParseResult(List.of(validRow(), errorRow()));
            assertEquals(1, result.errorCount());
        }

        @Test
        @DisplayName("Returns 2 when two error rows")
        void testTwoErrors() {
            CsvParseResult result = new CsvParseResult(List.of(errorRow(), errorRow()));
            assertEquals(2, result.errorCount());
        }
    }
}
