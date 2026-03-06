package edu.ntnu.idatt2003.g23.transaction;

import edu.ntnu.idatt2003.g23.model.Player;
import edu.ntnu.idatt2003.g23.model.Share;
import edu.ntnu.idatt2003.g23.transaction.calculator.SaleCalculator;

/**
 * Class representing a sale transaction
 */
public class Sale extends Transaction {
    /**
     * Constructor for Sale transaction
     * @param share being used in the transaction
     * @param week of the transaction
     */
    public Sale(Share share, int week) {
        super(share, week, new SaleCalculator(share));
    }

    @Override
    public void commit(Player player) {
        // TODO: Implement commit logic for sale transaction
    }
}
