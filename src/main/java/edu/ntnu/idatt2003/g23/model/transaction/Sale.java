package edu.ntnu.idatt2003.g23.model.transaction;

import java.math.BigDecimal;

import java.math.BigDecimal;

import edu.ntnu.idatt2003.g23.model.Player;
import edu.ntnu.idatt2003.g23.model.Share;
import edu.ntnu.idatt2003.g23.model.transaction.calculator.SaleCalculator;

/**
 * Class representing a sale transaction
 */
public class Sale extends Transaction {
  /**
   * Constructor for Sale transaction
   *
   * @param share being used in the transaction
   * @param week  of the transaction
   * @throws IllegalArgumentException if share is null or week is invalid
   */
  public Sale(Share share, int week) {
    super(share, week, new SaleCalculator(share));
  }

  public Sale(Share share, int week, BigDecimal taxRate) {
    super(share, week, new SaleCalculator(share, taxRate));
  }

  /**
   * Handles logic for completing the transaction
   *
   * @param player the player selling the share
   * @throws IllegalArgumentException if player is null
   * @throws IllegalStateException    if the transaction has already been committed or if the player does not own the share
   */
  @Override
  public void commit(Player player) {
    if (player == null) {
      throw new IllegalArgumentException("Player cannot be null");
    }
    if (isCommitted()) {
      throw new IllegalStateException("Transaction has already been committed");
    }

    BigDecimal cost = getCalculator().calculateTotal();

    if (!player.getPortfolio().getShares().contains(getShare())) {
      throw new IllegalStateException("Player does not own this share");
    }

    player.getPortfolio().removeShare(getShare());
    player.getTransactionArchive().add(this);
    player.addMoney(cost);
    committed = true;
  }
}
