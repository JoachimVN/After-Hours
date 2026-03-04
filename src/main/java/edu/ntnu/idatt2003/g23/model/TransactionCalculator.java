package edu.ntnu.idatt2003.g23.model;

import java.math.BigDecimal;

/**
 * Interface for calculating transaction costs.
 */
public interface TransactionCalculator {
    /**
     * Calculates the gross amount of the transaction.
     * @return
     */
    BigDecimal calculateGross();

    BigDecimal calculateCommission();

    BigDecimal calculateTax();

    BigDecimal calculateTotal();
}
