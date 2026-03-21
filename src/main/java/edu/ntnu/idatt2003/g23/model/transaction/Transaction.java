package edu.ntnu.idatt2003.g23.model.transaction;

import edu.ntnu.idatt2003.g23.model.Player;
import edu.ntnu.idatt2003.g23.model.Share;
import edu.ntnu.idatt2003.g23.model.transaction.calculator.TransactionCalculator;

/**
 * Abstract class representing a transaction
 */
public abstract class Transaction {
    private final Share share;
    private final int week;
    private final TransactionCalculator calculator;
    protected boolean committed;

    /**
     * Constructor for Transaction
     * @param share being used in the transaction
     * @param week of the transaction
     * @param calculator used to calculate costs for the transaction
     */
    protected Transaction(Share share, int week, TransactionCalculator calculator) {
        this.share = share;
        this.week = week;
        this.calculator = calculator;
        this.committed = false;
    }

    /**
     * Getter for the share used in the transaction
     * @return the share used in the transaction
     */
    public Share getShare() {
        return share;
    }

    /**
     * Getter for the week of the transaction
     * @return the week of the transaction
     */
    public int getWeek() {
        return week;
    }

    /**
     * Getter for the calculator used in the transaction
     * @return the calculator used in the transaction
     */
    public TransactionCalculator getCalculator() {
        return calculator;
    }

    /**
     * Checks if the transaction has been committed
     * @return true if the transaction has been committed, false otherwise
     */
    public boolean isCommitted() {
        return committed;
    }

    /**
     * Commits the transaction for the specified player.
     * This method executes the transaction, updating the player's portfolio and balance accordingly.
     * Once committed, the transaction cannot be committed again.
     *
     * @param player the player for whom the transaction is being committed
     * @throws IllegalStateException if the transaction has already been committed
     */
    public abstract void commit(Player player);
}
