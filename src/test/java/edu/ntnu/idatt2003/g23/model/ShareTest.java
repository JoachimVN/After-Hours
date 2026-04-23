package edu.ntnu.idatt2003.g23.model;

import edu.ntnu.idatt2003.g23.ModelTestFixtures;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ShareTest {

  @Test
  @DisplayName("Constructor stores fields correctly")
  void testConstructorStoresValues() {
    Stock stock = ModelTestFixtures.stock();
    Share share = new Share(stock, new BigDecimal("10"), new BigDecimal("150.00"));

    assertEquals(stock, share.getStock());
    assertEquals(new BigDecimal("10"), share.getQuantity());
    assertEquals(new BigDecimal("150.00"), share.getPurchasePrice());
  }

  @Nested
  @DisplayName("Constructor validation tests")
  class ConstructorValidationTests {

    @Test
    @DisplayName("Constructor throws on null stock")
    void testConstructorThrowsOnNullStock() {
      assertThrows(IllegalArgumentException.class,
          () -> new Share(null, new BigDecimal("5"), new BigDecimal("100")));
    }

    @Test
    @DisplayName("Constructor throws on null quantity")
    void testConstructorThrowsOnNullQuantity() {
      Stock stock = ModelTestFixtures.stock();
      assertThrows(IllegalArgumentException.class,
          () -> new Share(stock, null, new BigDecimal("100")));
    }

    @Test
    @DisplayName("Constructor throws on zero quantity")
    void testConstructorThrowsOnZeroQuantity() {
      Stock stock = ModelTestFixtures.stock();
      assertThrows(IllegalArgumentException.class,
          () -> new Share(stock, BigDecimal.ZERO, new BigDecimal("100")));
    }

    @Test
    @DisplayName("Constructor throws on negative quantity")
    void testConstructorThrowsOnNegativeQuantity() {
      Stock stock = ModelTestFixtures.stock();
      assertThrows(IllegalArgumentException.class,
          () -> new Share(stock, new BigDecimal("-5"), new BigDecimal("100")));
    }

    @Test
    @DisplayName("Constructor throws on null purchase price")
    void testConstructorThrowsOnNullPurchasePrice() {
      Stock stock = ModelTestFixtures.stock();
      assertThrows(IllegalArgumentException.class,
          () -> new Share(stock, new BigDecimal("10"), null));
    }

    @Test
    @DisplayName("Constructor throws on zero purchase price")
    void testConstructorThrowsOnZeroPurchasePrice() {
      Stock stock = ModelTestFixtures.stock();
      assertThrows(IllegalArgumentException.class,
          () -> new Share(stock, new BigDecimal("10"), BigDecimal.ZERO));
    }

    @Test
    @DisplayName("Constructor throws on negative purchase price")
    void testConstructorThrowsOnNegativePurchasePrice() {
      Stock stock = ModelTestFixtures.stock();
      assertThrows(IllegalArgumentException.class,
          () -> new Share(stock, new BigDecimal("10"), new BigDecimal("-1")));
    }
  }

  // ─── getOwnedShares ────────────────────────────────────────────────────────

  @Nested
  @DisplayName("getOwnedShares tests")
  class GetOwnedSharesTests {

    @Test
    @DisplayName("Returns empty list for empty input")
    void emptyInputReturnsEmptyList() {
      List<Share> result = Share.getOwnedShares(List.of());
      assertEquals(0, result.size());
    }

    @Test
    @DisplayName("Single share passes through unchanged (by symbol+qty)")
    void singleSharePassesThrough() {
      Stock stock =
          new Stock("AAPL", "Apple Inc.", new ArrayList<>(List.of(new BigDecimal("150"))));
      Share share = new Share(stock, new BigDecimal("5"), new BigDecimal("100"));

      List<Share> result = Share.getOwnedShares(List.of(share));

      assertEquals(1, result.size());
      assertEquals("AAPL", result.get(0).getStock().getSymbol());
      assertEquals(0, new BigDecimal("5").compareTo(result.get(0).getQuantity()));
    }

    @Test
    @DisplayName("Two shares of the same stock are consolidated")
    void twoSharesSameStockConsolidated() {
      Stock stock =
          new Stock("AAPL", "Apple Inc.", new ArrayList<>(List.of(new BigDecimal("150"))));
      Share s1 = new Share(stock, new BigDecimal("2"), new BigDecimal("100")); // cost = 200
      Share s2 = new Share(stock, new BigDecimal("3"), new BigDecimal("120")); // cost = 360

      List<Share> result = Share.getOwnedShares(List.of(s1, s2));

      // One consolidated share: qty = 5, avg purchase = 560/5 = 112
      assertEquals(1, result.size());
      assertEquals(0, new BigDecimal("5").compareTo(result.get(0).getQuantity()));
      assertEquals(0,
          new BigDecimal("112").compareTo(result.get(0).getPurchasePrice().stripTrailingZeros()));
    }

    @Test
    @DisplayName("Shares of different stocks produce two separate entries")
    void sharesOfDifferentStocksNotConsolidated() {
      Stock aapl = new Stock("AAPL", "Apple Inc.", new ArrayList<>(List.of(new BigDecimal("150"))));
      Stock googl =
          new Stock("GOOGL", "Google LLC", new ArrayList<>(List.of(new BigDecimal("200"))));
      Share s1 = new Share(aapl, new BigDecimal("2"), new BigDecimal("100"));
      Share s2 = new Share(googl, new BigDecimal("3"), new BigDecimal("190"));

      List<Share> result = Share.getOwnedShares(List.of(s1, s2));

      assertEquals(2, result.size());
      assertEquals("AAPL", result.get(0).getStock().getSymbol());
      assertEquals("GOOGL", result.get(1).getStock().getSymbol());
    }

    @Test
    @DisplayName("Preserves insertion order across symbols")
    void preservesInsertionOrder() {
      Stock a = new Stock("AAA", "Alpha", new ArrayList<>(List.of(new BigDecimal("10"))));
      Stock b = new Stock("BBB", "Beta", new ArrayList<>(List.of(new BigDecimal("20"))));
      Share s1 = new Share(b, new BigDecimal("1"), new BigDecimal("20"));
      Share s2 = new Share(a, new BigDecimal("1"), new BigDecimal("10"));

      List<Share> result = Share.getOwnedShares(List.of(s1, s2));

      assertEquals("BBB", result.get(0).getStock().getSymbol());
      assertEquals("AAA", result.get(1).getStock().getSymbol());
    }
  }
}
