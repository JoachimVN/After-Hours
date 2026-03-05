package IDATT2003.idatt2003_2026_23.src.main.java.edu.ntnu.idatt2003.g23.transaction;

import edu.ntnu.idatt2003.g23.transaction.calculator.TransactionCalculator;

public abstract class Transaction {
    private final Share share;
    private final int week;
    private final TransactionCalculator calculator;
    protected boolean committed;

    protected Transaction(Share share, int week, TransactionCalculator calculator) {
        this.share = share;
        this.week = week;
        this.calculator = calculator;
        this.committed = false;
    }

    public Share getShare() {
        return share;
    }

    public int getWeek() {
        return week;
    }

    public TransactionCalculator getCalculator() {
        return calculator;
    }

    public boolean isCommitted() {
        return committed;
    }

    public void commit(Player player) {
        // TODO: Implement commit logic
    }
}
