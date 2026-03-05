package edu.ntnu.idatt2003.g23.transaction.calculator;

import java.math.BigDecimal;

/**
 * Calculator for sales
 */
public class SaleCalculator implements TransactionCalculator {
    private static final BigDecimal COMMISSION_RATE = new BigDecimal("0.01");
    private static final BigDecimal TAX_RATE = new BigDecimal("0.3");

    private final BigDecimal purchasePrice;
    private final BigDecimal salePrice;
    private final BigDecimal quantity;

    /**
     * Constructor for SaleCalculator
     * @param share used to calculate costs
     */
    public SaleCalculator(Share share) {
        this.purchasePrice = share.getPurchasePrice();
        this.salePrice = share.getStock().getSalePrice();
        this.quantity = share.getQuantity();
    }

    /**
     * Calculates the gross amount for the sale
     * @return the gross amount
     */
    @Override
    public BigDecimal calculateGross() {
        return salePrice.multiply(quantity);
    }

    /**
     * Calculates the commission for the sale
     * @return the commission amount
     */
    @Override
    public BigDecimal calculateCommission() {
        return calculateGross().multiply(COMMISSION_RATE);
    }

    /**
     * Calculates the tax for the sale
     * @return the tax amount
     */
    @Override
    public BigDecimal calculateTax() {
        return (calculateGross().subtract(calculateCommission()).subtract(purchasePrice.multiply(quantity))).multiply(TAX_RATE);
    }

    /**
     * Calculates the total amount for the sale
     * @return the total amount
     */
    @Override
    public BigDecimal calculateTotal() {
        return calculateGross().subtract(calculateCommission()).subtract(calculateTax());
    }
    
}
