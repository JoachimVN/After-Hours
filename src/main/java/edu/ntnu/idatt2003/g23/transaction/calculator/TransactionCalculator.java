package edu.ntnu.idatt2003.g23.transaction.calculator;

import java.math.BigDecimal;

/**
 * Interface for calculating transaction costs
 */
public interface TransactionCalculator {
    /**
     * Calculates the gross amount of the transaction
     * @return the gross amount
     */
    BigDecimal calculateGross();

    /**
     * Calculates the commission for the transaction
     * @return the commission amount
     */
    BigDecimal calculateCommission();

    /**
     * Calculates the tax for the transaction
     * @return the tax amount
     */
    BigDecimal calculateTax();

    /**
     * Calculates the total amount for the transaction
     * @return the total amount
     */
    BigDecimal calculateTotal();
}
