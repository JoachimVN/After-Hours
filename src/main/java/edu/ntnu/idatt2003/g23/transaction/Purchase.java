package IDATT2003.idatt2003_2026_23.src.main.java.edu.ntnu.idatt2003.g23.transaction;

public class Purchase extends Transaction {
    public Purchase(Share share, int week) {
        super(share, week, new PurchaseCalculator(share));
    }

    @Override
    public void commit(Player player) {
        // TODO: Implement commit logic for purchase transaction
    }
}
