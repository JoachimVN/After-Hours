package IDATT2003.idatt2003_2026_23.src.main.java.edu.ntnu.idatt2003.g23.transaction;

public class Sale extends Transaction {
    public Sale(Share share, int week) {
        super(share, week, new SaleCalculator(share));
    }

    @Override
    public void commit(Player player) {
        // TODO: Implement commit logic for sale transaction
    }
}
