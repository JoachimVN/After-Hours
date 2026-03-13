package edu.ntnu.idatt2003.g23.model;

import java.math.BigDecimal;

import edu.ntnu.idatt2003.g23.model.transaction.TransactionArchive;

// Represents a player in the stock market game.
public class Player {
    private final String name;
    private final BigDecimal startingMoney;
    private BigDecimal money;
    private final Portfolio portfolio;
    private final TransactionArchive transactionArchive;

    // Constructor for creating a new Player instance.
    public Player(String name, BigDecimal startingMoney) {
        this.name = name;
        this.startingMoney = startingMoney;
        this.money = startingMoney;
        this.portfolio = new Portfolio();
        this.transactionArchive = new TransactionArchive();
    }

    /**
     * Gets the player's name.
     * @return the player's name
     * @throws IllegalArgumentException if the name is null
     */
    public String getName() {
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
            return name;
        }

    /**
     * Gets the player's current money.
     * @return the player's current money
     * @throws IllegalStateException if the  money is null or negative
     */
    public BigDecimal getMoney() {
        if (money == null || money.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Money cannot be null or negative");
        }
        return money;
    }

    /**
     * Gets the player's starting money.
     * @return the player's starting money
     * @throws IllegalStateException if the starting money is null or negative
     */
    public BigDecimal getStartingMoney() {
        if (startingMoney == null || startingMoney.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Starting money cannot be null or negative");
        }
        return startingMoney;
    }

    /**
     * Adds money to the player's current money.
     * @param amount the amount of money to add
     * @throws IllegalArgumentException if the amount is null or negative
     */
    public void addMoney(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be null or negative");
        }
        money = money.add(amount);
    }

    /**
     * Withdraws money from the player's current money.
     * @param amount the amount of money to withdraw
     * @throws IllegalArgumentException if the amount is null or negative
     * @throws IllegalStateException if the player does not have enough money
     */
    public void withdrawMoney(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be null or negative");
        }
        if (money.compareTo(amount) < 0) {
            throw new IllegalStateException("Not enough money to withdraw");
        }
        money = money.subtract(amount);
    }

    /**
     * Gets the player's portfolio.
     * @return the player's portfolio
     * @throws IllegalStateException if the portfolio is null
     */
    public Portfolio getPortfolio() {
        if (portfolio == null) {
            throw new IllegalStateException("Portfolio cannot be null");
        }
        return portfolio;
    }

    /**
     * Gets the player's transaction archive.
     * @return the player's transaction archive
     * @throws IllegalStateException if the transaction archive is null
     */
    public TransactionArchive getTransactionArchive() {
        if (transactionArchive == null) {
            throw new IllegalStateException("Transaction archive cannot be null");
        }
        return transactionArchive;
    }
}
