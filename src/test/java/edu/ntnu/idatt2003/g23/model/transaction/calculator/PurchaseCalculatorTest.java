package edu.ntnu.idatt2003.g23.model.transaction.calculator;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import edu.ntnu.idatt2003.g23.model.Share;
import edu.ntnu.idatt2003.g23.model.Stock;

class PurchaseCalculatorTest {

  @Test
  @DisplayName("Constructor stores share correctly")
  void testConstructor() {
    Stock stock = new Stock("AAPL", "Apple Inc.", List.of(new BigDecimal("150")));
    Share share = new Share(stock, new BigDecimal("10"), new BigDecimal("140"));

    PurchaseCalculator calculator = new PurchaseCalculator(share);

    // Test that calculations work (indirectly testing constructor)
    assertEquals(new BigDecimal("1400"), calculator.calculateGross()); // 140 * 10
  }

  @Test
  @DisplayName("calculateGross returns purchase price times quantity")
  void testCalculateGross() {
    Stock stock = new Stock("AAPL", "Apple Inc.", List.of(new BigDecimal("150")));
    Share share = new Share(stock, new BigDecimal("10"), new BigDecimal("140"));

    PurchaseCalculator calculator = new PurchaseCalculator(share);

    BigDecimal expected = new BigDecimal("140").multiply(new BigDecimal("10"));
    assertEquals(expected, calculator.calculateGross());
  }

  @Test
  @DisplayName("calculateCommission returns 0.5% of gross amount")
  void testCalculateCommission() {
    Stock stock = new Stock("AAPL", "Apple Inc.", List.of(new BigDecimal("150")));
    Share share = new Share(stock, new BigDecimal("10"), new BigDecimal("140"));

    PurchaseCalculator calculator = new PurchaseCalculator(share);

    BigDecimal gross = new BigDecimal("1400");
    BigDecimal expectedCommission = gross.multiply(new BigDecimal("0.005"));
    assertEquals(expectedCommission, calculator.calculateCommission());
  }

  @Test
  @DisplayName("calculateTax returns zero for purchases")
  void testCalculateTax() {
    Stock stock = new Stock("AAPL", "Apple Inc.", List.of(new BigDecimal("150")));
    Share share = new Share(stock, new BigDecimal("10"), new BigDecimal("140"));

    PurchaseCalculator calculator = new PurchaseCalculator(share);

    assertEquals(BigDecimal.ZERO, calculator.calculateTax());
  }

  @Test
  @DisplayName("calculateTotal returns gross plus commission plus tax")
  void testCalculateTotal() {
    Stock stock = new Stock("AAPL", "Apple Inc.", List.of(new BigDecimal("150")));
    Share share = new Share(stock, new BigDecimal("10"), new BigDecimal("140"));

    PurchaseCalculator calculator = new PurchaseCalculator(share);

    BigDecimal gross = new BigDecimal("1400");
    BigDecimal commission = gross.multiply(new BigDecimal("0.005"));
    BigDecimal tax = BigDecimal.ZERO;
    BigDecimal expectedTotal = gross.add(commission).add(tax);

    assertEquals(expectedTotal, calculator.calculateTotal());
  }

  @Test
  @DisplayName("calculateTotal with different values")
  void testCalculateTotalDifferentValues() {
    Stock stock = new Stock("GOOGL", "Google Inc.", List.of(new BigDecimal("2000")));
    Share share = new Share(stock, new BigDecimal("5"), new BigDecimal("1900"));

    PurchaseCalculator calculator = new PurchaseCalculator(share);

    BigDecimal gross = new BigDecimal("9500"); // 1900 * 5
    BigDecimal commission = gross.multiply(new BigDecimal("0.005")); // 47.5
    BigDecimal tax = BigDecimal.ZERO;
    BigDecimal expectedTotal = gross.add(commission).add(tax); // 9500 + 47.5 = 9547.5

    assertEquals(expectedTotal, calculator.calculateTotal());
  }

  @Test
  @DisplayName("Constructor throws on null share")
  void testConstructorThrowsOnNullShare() {
    assertThrows(IllegalArgumentException.class, () -> new PurchaseCalculator(null));
  }
}
