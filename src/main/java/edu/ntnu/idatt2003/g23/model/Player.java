package edu.ntnu.idatt2003.g23.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

import edu.ntnu.idatt2003.g23.model.transaction.TransactionArchive;

// Represents a player in the stock market game.
public class Player {
    private final String name;
    private final BigDecimal startingMoney;
    private BigDecimal money;
    private final Portfolio portfolio;
    private final TransactionArchive transactionArchive;
    private PlayerStatus status;

    // Constructor for creating a new Player instance.
    public Player(String name, BigDecimal startingMoney) {
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        if (name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be blank");
        }
        if (startingMoney == null) {
            throw new IllegalArgumentException("Starting money cannot be null");
        }
        if (startingMoney.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Starting money cannot be negative");
        }
        this.name = name;
        this.startingMoney = startingMoney;
        this.money = startingMoney;
        this.portfolio = new Portfolio();
        this.transactionArchive = new TransactionArchive();
        this.status = PlayerStatus.NOVICE;
    }

    /**
     * Gets the player's name.
     * @return the player's name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the player's current money.
     * @return the player's current money
     */
    public BigDecimal getMoney() {
        return money;
    }

    /**
     * Gets the player's starting money.
     * @return the player's starting money
     */
    public BigDecimal getStartingMoney() {
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
     */
    public Portfolio getPortfolio() {
        return portfolio;
    }

    /**
     * Gets the player's transaction archive.
     * @return the player's transaction archive
     */
    public TransactionArchive getTransactionArchive() {
        return transactionArchive;
    }

    /**
     * Gets the player's net worth.
     * @return the player's net worth
     */
    public BigDecimal getNetWorth() {
        return money.add(portfolio.getNetWorth());
    }

    /**
     * Gets the number of distinct weeks the player has traded.
     * @return the number of distinct weeks the player has traded
     */
    public int getWeeksTraded() {
        return transactionArchive.countDistinctWeeks();
    }

    /**
     * Calculates the player's status based on their net worth growth and amount of weeks traded
     */
    public void calculateStatus() {
        int weeks = getWeeksTraded();
        BigDecimal netWorth = getNetWorth();

        if (startingMoney.compareTo(BigDecimal.ZERO) == 0) {
            status = PlayerStatus.NOVICE;
        } else {
            BigDecimal growth = netWorth.divide(startingMoney, 4, RoundingMode.HALF_UP);

            if (weeks >= 20 &&  growth.compareTo(BigDecimal.valueOf(2.0)) >= 0) {
                status = PlayerStatus.SPECULATOR;
            } else if (weeks >= 10 && growth.compareTo(BigDecimal.valueOf(1.2)) >= 0) {
                status = PlayerStatus.INVESTOR;
            } else {
                status = PlayerStatus.NOVICE;
            }
        }        
    }

    /**
     * Gets the player's status.
     * @return the player's status
     */
    public PlayerStatus getStatus() {
        return status;
    }
}