package edu.ntnu.idatt2003.g23.model.transaction;

import java.math.BigDecimal;

import edu.ntnu.idatt2003.g23.model.Share;

/**
 * Factory for creating transaction objects.
 */
public final class TransactionFactory {
  private TransactionFactory() {
    throw new IllegalStateException("TransactionFactory is a utility class");
  }

  public enum TransactionType {
    PURCHASE,
    SALE
  }

  /**
   * Creates a purchase transaction for the given share and week.
   *
   * @param share the share to purchase
   * @param week  the game week of the transaction
   * @return a new Purchase transaction
   */
  public static Transaction createPurchase(Share share, int week) {
    return new Purchase(share, week);
  }

  /**
   * Creates a sale transaction for the given share and week.
   *
   * @param share the share to sell
   * @param week  the game week of the transaction
   * @return a new Sale transaction
   */
  public static Transaction createSale(Share share, int week) {
    return new Sale(share, week);
  }

  public static Transaction createSale(Share share, int week, BigDecimal taxRate) {
    return new Sale(share, week, taxRate);
  }

  /**
   * Generic factory entry point for both purchase and sale.
   *
   * @param type  the transaction type
   * @param share the share to use
   * @param week  the game week
   * @return a new Transaction instance
   */
  public static Transaction createTransaction(TransactionType type, Share share, int week) {
    if (type == null) {
      throw new IllegalArgumentException("Transaction type cannot be null");
    }

    return switch (type) {
      case PURCHASE -> createPurchase(share, week);
      case SALE -> createSale(share, week);
    };
  }
}