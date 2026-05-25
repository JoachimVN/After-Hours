package edu.ntnu.idatt2003.g23.ui.util;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CurrencyFormatterTest {

  @Test
  void format_appliesCurrencySymbolThousandsAndTwoDecimals() {
    assertEquals("$12,345.68", CurrencyFormatter.format(new BigDecimal("12345.678")));
  }

  @Test
  void format_handlesNegativeValues() {
    assertEquals("$-1,234.50", CurrencyFormatter.format(new BigDecimal("-1234.5")));
  }
}
