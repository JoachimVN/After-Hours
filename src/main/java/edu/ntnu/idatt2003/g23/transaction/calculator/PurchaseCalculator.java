package edu.ntnu.idatt2003.g23.transaction.calculator;

import java.math.BigDecimal;

public class PurchaseCalculator implements TransactionCalculator {
    private static final BigDecimal COMMISSION_RATE = new BigDecimal("0.005");
    private static final BigDecimal TAX_RATE = new BigDecimal("0.3");

    private final BigDecimal purchasePrice;
    private final BigDecimal quantity;

    public PurchaseCalculator(Share share) {
        this.purchasePrice = share.getPurchasePrice();
        this.quantity = share.getQuantity();
    }

    @Override
    public BigDecimal calculateGross() {
        return purchasePrice.multiply(quantity);
    }

    @Override
    public BigDecimal calculateCommission() {
        return calculateGross().multiply(COMMISSION_RATE);
    }

    @Override
    public BigDecimal calculateTax() {
        return new BigDecimal("0");
    }

    @Override
    public BigDecimal calculateTotal() {
        return calculateGross().add(calculateCommission()).add(calculateTax());
    }
}
