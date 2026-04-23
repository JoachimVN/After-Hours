package edu.ntnu.idatt2003.g23.ui.util;

import java.math.BigDecimal;

public class CurrencyFormatter {
  public static String format(BigDecimal value) {
    return String.format("$%,.2f", value.doubleValue());
  }
}
