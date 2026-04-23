package edu.ntnu.idatt2003.g23.model.transaction.calculator;

import java.math.BigDecimal;

import edu.ntnu.idatt2003.g23.model.Share;

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
   *
   * @param share used to calculate costs
   * @throws IllegalArgumentException if share is null
   */
  public SaleCalculator(Share share) {
    if (share == null) {
      throw new IllegalArgumentException("Share cannot be null");
    }
    this.purchasePrice = share.getPurchasePrice();
    this.salePrice = share.getStock().getSalesPrice();
    this.quantity = share.getQuantity();
  }

  /**
   * Calculates the gross amount for the sale
   *
   * @return the gross amount
   */
  @Override
  public BigDecimal calculateGross() {
    return salePrice.multiply(quantity);
  }

  /**
   * Calculates the commission for the sale
   *
   * @return the commission amount
   */
  @Override
  public BigDecimal calculateCommission() {
    return calculateGross().multiply(COMMISSION_RATE);
  }

  /**
   * Calculates the tax for the sale
   *
   * @return the tax amount
   */
  @Override
  public BigDecimal calculateTax() {
    BigDecimal profit =
        calculateGross().subtract(calculateCommission()).subtract(purchasePrice.multiply(quantity));
    if (profit.compareTo(BigDecimal.ZERO) <= 0) {
      return BigDecimal.ZERO;
    }
    return profit.multiply(TAX_RATE);
  }

  /**
   * Calculates the total amount for the sale
   *
   * @return the total amount
   */
  @Override
  public BigDecimal calculateTotal() {
    return calculateGross().subtract(calculateCommission()).subtract(calculateTax());
  }

}
