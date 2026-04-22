package edu.ntnu.idatt2003.g23.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

import edu.ntnu.idatt2003.g23.model.transaction.TransactionArchive;

// Represents a player in the stock market game.
public class Player {
    private static final int INVESTOR_WEEKS_REQUIRED = 10;
    private static final BigDecimal INVESTOR_GROWTH_REQUIRED = BigDecimal.valueOf(1.2);
    private static final int SPECULATOR_WEEKS_REQUIRED = 20;
    private static final BigDecimal SPECULATOR_GROWTH_REQUIRED = BigDecimal.valueOf(2.0);

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

            if (weeks >= SPECULATOR_WEEKS_REQUIRED &&  growth.compareTo(SPECULATOR_GROWTH_REQUIRED) >= 0) {
                status = PlayerStatus.SPECULATOR;
            } else if (weeks >= INVESTOR_WEEKS_REQUIRED && growth.compareTo(INVESTOR_GROWTH_REQUIRED) >= 0) {
                status = PlayerStatus.INVESTOR;
            } else {
                status = PlayerStatus.NOVICE;
            }
        }        
    }

    /**
     * Calculates the player's progress towards the next status level as a value between 0 and 1.
     * @return the progress towards the next status level
     */
    public BigDecimal calculateStatusProgress() {
        if (startingMoney.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        if (status == PlayerStatus.SPECULATOR) {
            return BigDecimal.ONE;
        }
        BigDecimal weeksProgress = calculateWeeksProgress();
        BigDecimal networthProgress = calculateNetWorthProgress();
        return weeksProgress.add(networthProgress).divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateNetWorthProgress() {
        if (startingMoney.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal progress = BigDecimal.ZERO;
        switch (status) {
            case NOVICE -> {
                progress = getNetWorth().subtract(startingMoney).divide(startingMoney.multiply(INVESTOR_GROWTH_REQUIRED).subtract(startingMoney), 4, RoundingMode.HALF_UP);
            }
            case INVESTOR -> {
                progress = getNetWorth().subtract(startingMoney.multiply(INVESTOR_GROWTH_REQUIRED)).divide(startingMoney.multiply(SPECULATOR_GROWTH_REQUIRED).subtract(startingMoney.multiply(INVESTOR_GROWTH_REQUIRED)), 4, RoundingMode.HALF_UP);
            }
            case SPECULATOR -> {
                progress = BigDecimal.ONE;
            }
            default -> throw new IllegalStateException("Unexpected value: " + status);
        }

        if (progress.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        } else if (progress.compareTo(BigDecimal.ONE) > 0) {
            return BigDecimal.ONE;
        } else {
            return progress;
        }
    }

    public BigDecimal calculateWeeksProgress() {
        BigDecimal progress = BigDecimal.ZERO;
        switch (status) {
            case NOVICE -> {
                progress = BigDecimal.valueOf(getWeeksTraded()).divide(BigDecimal.valueOf(INVESTOR_WEEKS_REQUIRED), 4, RoundingMode.HALF_UP);
            }
            case INVESTOR -> {
                progress = BigDecimal.valueOf(getWeeksTraded() - INVESTOR_WEEKS_REQUIRED).divide(BigDecimal.valueOf(SPECULATOR_WEEKS_REQUIRED - INVESTOR_WEEKS_REQUIRED), 4, RoundingMode.HALF_UP);
            }
            case SPECULATOR -> {
                progress = BigDecimal.ONE;
            }
            default -> throw new IllegalStateException("Unexpected value: " + status);
        }

        if (progress.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        } else if (progress.compareTo(BigDecimal.ONE) > 0) {
            return BigDecimal.ONE;
        } else {
            return progress;
        }
    }

    public BigDecimal getNetWorthGrowthRatio() {
        if (startingMoney.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return getNetWorth().divide(startingMoney, 4, RoundingMode.HALF_UP);
    }

    public BigDecimal getGrowthTargetForNextStatus() {
        return switch (status) {
            case NOVICE -> INVESTOR_GROWTH_REQUIRED;
            case INVESTOR -> SPECULATOR_GROWTH_REQUIRED;
            case SPECULATOR -> SPECULATOR_GROWTH_REQUIRED;
        };
    }

    public int getWeeksTargetForNextStatus() {
        return switch (status) {
            case NOVICE -> INVESTOR_WEEKS_REQUIRED;
            case INVESTOR -> SPECULATOR_WEEKS_REQUIRED;
            case SPECULATOR -> SPECULATOR_WEEKS_REQUIRED;
        };
    }
    /**
     * Gets the player's status.
     * @return the player's status
     */
    public PlayerStatus getStatus() {
        return status;
    }

    /**
     * Checks if the player owns any shares of a stock with the given symbol
     * @param symbol the stock symbol to check
     * @return true if the player owns any shares of the stock, false otherwise
     */
    public boolean ownsStock(String symbol) {
        return portfolio.getShareBySymbol(symbol)
            .stream().map(Share::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add)
            .compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Directly sets the player's current money. Used when restoring a saved game.
     * @param money the money value to set
     */
    public void setMoney(BigDecimal money) {
        if (money == null || money.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Money cannot be null or negative");
        }
        this.money = money;
    }

    /**
     * Directly sets the player's status. Used when restoring a saved game.
     * @param status the status to set
     */
    public void setStatus(PlayerStatus status) {
        if (status == null) throw new IllegalArgumentException("Status cannot be null");
        this.status = status;
    }
}