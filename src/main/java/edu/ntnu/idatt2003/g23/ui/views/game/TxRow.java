package edu.ntnu.idatt2003.g23.ui.views.game;

import java.math.BigDecimal;

public record TxRow(int week, boolean isBuy, String symbol, String company,
                     BigDecimal quantity, BigDecimal pricePerShare,
                     BigDecimal fee, BigDecimal tax, BigDecimal total) {}
