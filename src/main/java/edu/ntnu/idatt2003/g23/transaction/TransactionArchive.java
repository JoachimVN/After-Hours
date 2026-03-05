package edu.ntnu.idatt2003.g23.transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TransactionArchive {
    private List<Transaction> transactions;

    public TransactionArchive() {
        this.transactions = new ArrayList<>();
    }

    public boolean add(Transaction transaction) {
        if (transaction == null) {
            return false;
        }
        return transactions.add(transaction);
    }

    public boolean isEmpty() {
        return transactions.isEmpty();
    }

    public List<Transaction> getTransactions(int week) {
        return transactions.stream()                                        // AI
                .filter(t -> t.getWeek() == week)
                .collect(Collectors.toList());
    }

    public List<Purchase> getPurchases(int week) {
        return transactions.stream()                                        // AI
                .filter(t -> t instanceof Purchase && t.getWeek() == week)
                .map(t -> (Purchase) t)
                .collect(Collectors.toList());
    }
}