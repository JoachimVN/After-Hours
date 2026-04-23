package edu.ntnu.idatt2003.g23.model.transaction;

import java.math.BigDecimal;

import edu.ntnu.idatt2003.g23.model.Player;
import edu.ntnu.idatt2003.g23.model.Share;
import edu.ntnu.idatt2003.g23.model.transaction.calculator.PurchaseCalculator;

/**
 * Class representing a purchase transaction
 */
public class Purchase extends Transaction {
  /**
   * Constructor for Purchase transaction
   *
   * @param share being used in the transaction
   * @param week  of the transaction
   * @throws IllegalArgumentException if share is null or week is invalid
   */
  public Purchase(Share share, int week) {
    super(share, week, new PurchaseCalculator(share));
  }

  /**
   * Handles logic for completing the transaction
   *
   * @param player the player buying the share
   * @throws IllegalArgumentException if player is null
   * @throws IllegalStateException    if the transaction has already been committed or if the player does not have enough money
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

    if (player.getMoney().compareTo(cost) < 0) {
      throw new IllegalStateException("Player does not have enough money");
    }

    player.getPortfolio().addShare(getShare());
    player.getTransactionArchive().add(this);
    player.withdrawMoney(cost);
    committed = true;
  }
}
