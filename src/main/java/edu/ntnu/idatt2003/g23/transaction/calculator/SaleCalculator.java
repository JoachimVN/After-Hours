package edu.ntnu.idatt2003.g23.transaction.calculator;

import java.math.BigDecimal;

public class SaleCalculator implements TransactionCalculator {
    private static final BigDecimal COMMISSION_RATE = new BigDecimal("0.01");
    private static final BigDecimal TAX_RATE = new BigDecimal("0.3");

    private final BigDecimal purchasePrice;
    private final BigDecimal salePrice;
    private final BigDecimal quantity;

    public SaleCalculator(Share share) {
        this.purchasePrice = share.getPurchasePrice();
        this.salePrice = share.getStock().getSalePrice();
        this.quantity = share.getQuantity();
    }

    @Override
    public BigDecimal calculateGross() {
        return salePrice.multiply(quantity);
    }

    @Override
    public BigDecimal calculateCommission() {
        return calculateGross().multiply(COMMISSION_RATE);
    }

    @Override
    public BigDecimal calculateTax() {
        return (calculateGross().subtract(calculateCommission()).subtract(purchasePrice.multiply(quantity))).multiply(TAX_RATE);
    }

    @Override
    public BigDecimal calculateTotal() {
        return calculateGross().subtract(calculateCommission()).subtract(calculateTax());
    }
    
}
