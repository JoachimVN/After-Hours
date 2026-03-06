package edu.ntnu.idatt2003.g23.transaction;

import edu.ntnu.idatt2003.g23.model.Player;
import edu.ntnu.idatt2003.g23.model.Share;
import edu.ntnu.idatt2003.g23.transaction.calculator.PurchaseCalculator;

/**
 * Class representing a purchase transaction
 */
public class Purchase extends Transaction {
    /**
     * Constructor for Purchase transaction
     * @param share being used in the transaction
     * @param week of the transaction
     */
    public Purchase(Share share, int week) {
        super(share, week, new PurchaseCalculator(share));
    }

    @Override
    public void commit(Player player) {
        // TODO: Implement commit logic for purchase transaction
    }
}
