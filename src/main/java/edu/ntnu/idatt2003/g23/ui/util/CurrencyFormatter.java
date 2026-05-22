package edu.ntnu.idatt2003.g23.ui.util;

import java.math.BigDecimal;
import java.util.Locale;

public class CurrencyFormatter {
  public static String format(BigDecimal value) {
    return String.format(Locale.US, "$%,.2f", value.doubleValue());
  }
}
