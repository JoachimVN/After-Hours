package edu.ntnu.idatt2003.g23.transaction.calculator;

import java.math.BigDecimal;
import edu.ntnu.idatt2003.g23.model.Share;

/**
 * Calculator for purchases
 */
public class PurchaseCalculator implements TransactionCalculator {
    private static final BigDecimal COMMISSION_RATE = new BigDecimal("0.005");
    private static final BigDecimal TAX_RATE = new BigDecimal("0.3");

    private final BigDecimal purchasePrice;
    private final BigDecimal quantity;

    /**
     * Constructor for PurchaseCalculator
     * @param share used to calculate costs
     */
    public PurchaseCalculator(Share share) {
        this.purchasePrice = share.getPurchasePrice();
        this.quantity = share.getQuantity();
    }

    /**
     * Calculates the gross amount for the purchase
     * @return the gross amount
     */
    @Override
    public BigDecimal calculateGross() {
        return purchasePrice.multiply(quantity);
    }

    /**
     * Calculates the commission for the purchase
     * @return the commission amount
     */
    @Override
    public BigDecimal calculateCommission() {
        return calculateGross().multiply(COMMISSION_RATE);
    }

    /**
     * Calculates the tax for the purchase
     * @return the tax amount
     */
    @Override
    public BigDecimal calculateTax() {
        return new BigDecimal("0");
    }

    /**
     * Calculates the total amount for the purchase
     * @return the total amount
     */
    @Override
    public BigDecimal calculateTotal() {
        return calculateGross().add(calculateCommission()).add(calculateTax());
    }
}
