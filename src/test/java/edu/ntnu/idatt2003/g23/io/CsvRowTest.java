package edu.ntnu.idatt2003.g23.io;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CsvRowTest {

  private CsvRow valid() {
    return new CsvRow(1, "AAPL", "Apple Inc.", "150.00", "");
  }

  @Nested
  @DisplayName("Constructor")
  class ConstructorTests {

    @Test
    @DisplayName("Stores all values correctly")
    void testStoresValues() {
      CsvRow row = new CsvRow(3, "TSLA", "Tesla Inc.", "200.00;210.00", "bad symbol");
      assertEquals(3, row.getLineNumber());
      assertEquals("TSLA", row.getSymbol());
      assertEquals("Tesla Inc.", row.getCompany());
      assertEquals("200.00;210.00", row.getPrices());
      assertEquals("bad symbol", row.getErrorMessage());
      assertEquals("", row.getErrorColumn());
    }

    @Test
    @DisplayName("Converts null fields to empty string")
    void testNullFieldsBecomEmpty() {
      CsvRow row = new CsvRow(1, null, null, null, null);
      assertEquals("", row.getSymbol());
      assertEquals("", row.getCompany());
      assertEquals("", row.getPrices());
      assertEquals("", row.getErrorMessage());
    }
  }

  @Nested
  @DisplayName("hasError")
  class HasErrorTests {

    @Test
    @DisplayName("Returns false when errorMessage is empty")
    void testNoError() {
      assertFalse(valid().hasError());
    }

    @Test
    @DisplayName("Returns true when errorMessage is non-empty")
    void testHasError() {
      CsvRow row = new CsvRow(1, "AAPL", "Apple Inc.", "150.00", "some error");
      assertTrue(row.hasError());
    }
  }

  @Nested
  @DisplayName("Getters, setters, and properties")
  class AccessorTests {

    @Test
    @DisplayName("lineNumberProperty returns property with correct value")
    void testLineNumberProperty() {
      CsvRow row = valid();
      assertEquals(1, row.lineNumberProperty().get());
    }

    @Test
    @DisplayName("setSymbol updates value; null becomes empty string")
    void testSetSymbol() {
      CsvRow row = valid();
      row.setSymbol("GOOG");
      assertEquals("GOOG", row.getSymbol());
      row.setSymbol(null);
      assertEquals("", row.getSymbol());
    }

    @Test
    @DisplayName("symbolProperty returns observable property")
    void testSymbolProperty() {
      CsvRow row = valid();
      assertEquals("AAPL", row.symbolProperty().get());
    }

    @Test
    @DisplayName("setCompany updates value; null becomes empty string")
    void testSetCompany() {
      CsvRow row = valid();
      row.setCompany("Google LLC");
      assertEquals("Google LLC", row.getCompany());
      row.setCompany(null);
      assertEquals("", row.getCompany());
    }

    @Test
    @DisplayName("companyProperty returns observable property")
    void testCompanyProperty() {
      CsvRow row = valid();
      assertEquals("Apple Inc.", row.companyProperty().get());
    }

    @Test
    @DisplayName("setPrices updates value; null becomes empty string")
    void testSetPrices() {
      CsvRow row = valid();
      row.setPrices("99.00;100.00");
      assertEquals("99.00;100.00", row.getPrices());
      row.setPrices(null);
      assertEquals("", row.getPrices());
    }

    @Test
    @DisplayName("pricesProperty returns observable property")
    void testPricesProperty() {
      CsvRow row = valid();
      assertEquals("150.00", row.pricesProperty().get());
    }

    @Test
    @DisplayName("setErrorMessage updates value; null becomes empty string")
    void testSetErrorMessage() {
      CsvRow row = valid();
      row.setErrorMessage("oops");
      assertEquals("oops", row.getErrorMessage());
      row.setErrorMessage(null);
      assertEquals("", row.getErrorMessage());
    }

    @Test
    @DisplayName("errorMessageProperty returns observable property")
    void testErrorMessageProperty() {
      CsvRow row = new CsvRow(1, "AAPL", "Apple Inc.", "150.00", "err");
      assertEquals("err", row.errorMessageProperty().get());
    }

    @Test
    @DisplayName("setErrorColumn updates value; null becomes empty string")
    void testSetErrorColumn() {
      CsvRow row = valid();
      row.setErrorColumn("symbol");
      assertEquals("symbol", row.getErrorColumn());
      row.setErrorColumn(null);
      assertEquals("", row.getErrorColumn());
    }

    @Test
    @DisplayName("errorColumnProperty returns observable property")
    void testErrorColumnProperty() {
      CsvRow row = valid();
      row.setErrorColumn("prices");
      assertEquals("prices", row.errorColumnProperty().get());
    }
  }
}
