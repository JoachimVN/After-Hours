package edu.ntnu.idatt2003.g23.model.transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A class used to store and manage transactions
 */
public class TransactionArchive {
    private List<Transaction> transactions;

    /**
     * Constructor for TransactionArchive
     */
    public TransactionArchive() {
        this.transactions = new ArrayList<>();
    }

    /**
     * Adds a transaction to the archive
     * @param transaction
     * @return true if the transaction was added successfully, otherwise returns false
     */
    public boolean add(Transaction transaction) {
        if (transaction == null) {
            return false;
        }
        return transactions.add(transaction);
    }

    /**
     * Checks if the archive is empty
     * @return true if the archive is empty, otherwise returns false
     */
    public boolean isEmpty() {
        return transactions.isEmpty();
    }

    /**
     * Gets every transactions from a given week
     * @param week to get transactions from
     * @return a list of transactions from the given week
     * @throws IllegalArgumentException if week is not positive
     */
    public List<Transaction> getTransactions(int week) {
        if (week <= 0) {
            throw new IllegalArgumentException("Week must be positive");
        }
        return transactions.stream()
                .filter(t -> t.getWeek() == week)
                .collect(Collectors.toList());
    }

    /**
     * Gets every purchase from a given week
     * @param week to get purchases from
     * @return a list of purchases from the given week
     * @throws IllegalArgumentException if week is not positive
     */
    public List<Purchase> getPurchases(int week) {
        if (week <= 0) {
            throw new IllegalArgumentException("Week must be positive");
        }
        return transactions.stream()                                        // AI
                .filter(t -> t instanceof Purchase && t.getWeek() == week)
                .map(t -> (Purchase) t)
                .collect(Collectors.toList());
    }

    /**
     * Gets every sale from a given week
     * @param week to get sales from
     * @return a list of sales from the given week
     * @throws IllegalArgumentException if week is not positive
     */
    public List<Sale> getSales(int week) {
        if (week <= 0) {
            throw new IllegalArgumentException("Week must be positive");
        }
        return transactions.stream()                                        // AI
                .filter(t -> t instanceof Sale && t.getWeek() == week)
                .map(t -> (Sale) t)
                .collect(Collectors.toList());
    }

    /**
     * Counts the number of weeks there have been transactions
     * @return the amount of weeks there have been transactions
     */
    public int countDistinctWeeks() {
        return (int) transactions.stream()                                  // AI
                .map(Transaction::getWeek)
                .distinct()
                .count();
    }
}