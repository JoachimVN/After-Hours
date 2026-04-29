package edu.ntnu.idatt2003.g23.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import edu.ntnu.idatt2003.g23.model.transaction.TransactionArchive;

// Represents a player in the stock market game.
public class Player {
  public record WeeklySnapshot(int week, BigDecimal cash, BigDecimal portfolioValue,
                               BigDecimal netWorth) {
  }

  private static final String DEFAULT_PROFILE_AVATAR = "\uD83E\uDDD1";
  private static final String CHICK_AVATAR_SELECTION = "egg";
  private static final int CHICK_MAX_PHASE = 3;
  private static final int CHICK_PHASE_WEEKS = 10;
  private static final int CHICK_MAX_WEEKS = 30;

  private String name;
  private final BigDecimal startingMoney;
  private BigDecimal money;
  private final Portfolio portfolio;
  private final TransactionArchive transactionArchive;
  private final List<WeeklySnapshot> weeklySnapshots;
  private PlayerStatus status;
  private String profileAvatar;

  // Chicks avatar progression
  private int weeksUsingChickAvatar = 0;

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
    this.weeklySnapshots = new ArrayList<>();
    this.status = PlayerStatus.NOVICE;
    this.profileAvatar = DEFAULT_PROFILE_AVATAR;
    recordWeeklySnapshot(1);
  }

  public void recordWeeklySnapshot(int week) {
    if (week < 1) {
      throw new IllegalArgumentException("Week must be positive");
    }
    BigDecimal portfolioValue = portfolio.getNetWorth();
    BigDecimal netWorth = money.add(portfolioValue);
    WeeklySnapshot snapshot = new WeeklySnapshot(week, money, portfolioValue, netWorth);

    for (int i = 0; i < weeklySnapshots.size(); i++) {
      if (weeklySnapshots.get(i).week() == week) {
        weeklySnapshots.set(i, snapshot);
        return;
      }
    }
    weeklySnapshots.add(snapshot);
    weeklySnapshots.sort(Comparator.comparingInt(WeeklySnapshot::week));
  }

  public List<WeeklySnapshot> getWeeklySnapshots() {
    return List.copyOf(weeklySnapshots);
  }

  public void setWeeklySnapshots(List<WeeklySnapshot> snapshots) {
    weeklySnapshots.clear();
    if (snapshots == null) {
      return;
    }
    for (WeeklySnapshot snapshot : snapshots) {
      if (snapshot == null) {
        continue;
      }
      if (snapshot.week() < 1) {
        continue;
      }
      weeklySnapshots.add(snapshot);
    }
    weeklySnapshots.sort(Comparator.comparingInt(WeeklySnapshot::week));
  }

  /**
   * Gets the player's name.
   *
   * @return the player's name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the player's name.
   *
   * @param name the new player name (must not be null or blank)
   * @throws IllegalArgumentException if the name is null or blank
   */
  public void setName(String name) {
    if (name == null) {
      throw new IllegalArgumentException("Name cannot be null");
    }
    if (name.isBlank()) {
      throw new IllegalArgumentException("Name cannot be blank");
    }
    this.name = name;
  }

  /**
   * Gets the player's current money.
   *
   * @return the player's current money
   */
  public BigDecimal getMoney() {
    return money;
  }

  /**
   * Gets the player's starting money.
   *
   * @return the player's starting money
   */
  public BigDecimal getStartingMoney() {
    return startingMoney;
  }

  /**
   * Adds money to the player's current money.
   *
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
   *
   * @param amount the amount of money to withdraw
   * @throws IllegalArgumentException if the amount is null or negative
   * @throws IllegalStateException    if the player does not have enough money
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
   *
   * @return the player's portfolio
   */
  public Portfolio getPortfolio() {
    return portfolio;
  }

  /**
   * Gets the player's transaction archive.
   *
   * @return the player's transaction archive
   */
  public TransactionArchive getTransactionArchive() {
    return transactionArchive;
  }

  /**
   * Gets the player's net worth.
   *
   * @return the player's net worth
   */
  public BigDecimal getNetWorth() {
    return money.add(portfolio.getNetWorth());
  }

  /**
   * Gets the number of distinct weeks the player has traded.
   *
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

      if (weeks >= PlayerStatus.INVESTOR.getWeeksTargetForNextStatus()
          && growth.compareTo(PlayerStatus.INVESTOR.getGrowthTargetForNextStatus()) >= 0) {
        status = PlayerStatus.SPECULATOR;
      } else if (weeks >= PlayerStatus.NOVICE.getWeeksTargetForNextStatus()
          && growth.compareTo(PlayerStatus.NOVICE.getGrowthTargetForNextStatus()) >= 0) {
        status = PlayerStatus.INVESTOR;
      } else {
        status = PlayerStatus.NOVICE;
      }
    }
  }

  /**
   * Calculates the player's progress towards the next status level as a value between 0 and 1.
   *
   * @return the progress towards the next status level
   */
  public BigDecimal calculateStatusProgress() {
    return calculateStatusProgress(getNextStatusTarget());
  }

  public BigDecimal calculateStatusProgress(PlayerStatus targetStatus) {
    if (startingMoney.compareTo(BigDecimal.ZERO) == 0) {
      return BigDecimal.ZERO;
    }
    if (targetStatus == null) {
      throw new IllegalArgumentException("targetStatus cannot be null");
    }
    calculateStatus();
    if (targetStatus == PlayerStatus.NOVICE || status.ordinal() >= targetStatus.ordinal()) {
      return BigDecimal.ONE;
    }
    BigDecimal weeksProgress = calculateWeeksProgress(targetStatus);
    BigDecimal networthProgress = calculateNetWorthProgress(targetStatus);
    BigDecimal progress = weeksProgress.add(networthProgress)
        .divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
    if (progress.compareTo(BigDecimal.ONE) == 0) {
      return BigDecimal.ONE;
    }
    return progress;
  }

  public BigDecimal calculateNetWorthProgress() {
    return calculateNetWorthProgress(getNextStatusTarget());
  }

  public BigDecimal calculateNetWorthProgress(PlayerStatus targetStatus) {
    if (startingMoney.compareTo(BigDecimal.ZERO) == 0) {
      return BigDecimal.ZERO;
    }
    if (targetStatus == null) {
      throw new IllegalArgumentException("targetStatus cannot be null");
    }
    calculateStatus();
    if (targetStatus == PlayerStatus.NOVICE || status.ordinal() >= targetStatus.ordinal()) {
      return BigDecimal.ONE;
    }

    BigDecimal progress = BigDecimal.ZERO;
    switch (targetStatus) {
      case INVESTOR -> {
        progress = getNetWorth().subtract(startingMoney)
            .divide(startingMoney.multiply(PlayerStatus.NOVICE.getGrowthTargetForNextStatus())
                    .subtract(startingMoney),
                4,
                RoundingMode.HALF_UP);
      }
      case SPECULATOR -> {
        BigDecimal investorGrowthTarget = PlayerStatus.NOVICE.getGrowthTargetForNextStatus();
        BigDecimal speculatorGrowthTarget = PlayerStatus.INVESTOR.getGrowthTargetForNextStatus();
        progress = getNetWorth().subtract(startingMoney.multiply(investorGrowthTarget))
            .divide(
                startingMoney.multiply(speculatorGrowthTarget)
                    .subtract(startingMoney.multiply(investorGrowthTarget)),
                4,
                RoundingMode.HALF_UP);
      }
      case NOVICE -> {
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
    return calculateWeeksProgress(getNextStatusTarget());
  }

  public BigDecimal calculateWeeksProgress(PlayerStatus targetStatus) {
    if (targetStatus == null) {
      throw new IllegalArgumentException("targetStatus cannot be null");
    }
    calculateStatus();
    if (targetStatus == PlayerStatus.NOVICE || status.ordinal() >= targetStatus.ordinal()) {
      return BigDecimal.ONE;
    }

    BigDecimal progress = BigDecimal.ZERO;
    switch (targetStatus) {
      case INVESTOR -> {
        progress = BigDecimal.valueOf(getWeeksTraded())
            .divide(BigDecimal.valueOf(PlayerStatus.NOVICE.getWeeksTargetForNextStatus()), 4,
                RoundingMode.HALF_UP);
      }
      case SPECULATOR -> {
        int noviceWeeksTarget = PlayerStatus.NOVICE.getWeeksTargetForNextStatus();
        int investorWeeksTarget = PlayerStatus.INVESTOR.getWeeksTargetForNextStatus();
        progress = BigDecimal.valueOf(getWeeksTraded() - noviceWeeksTarget)
            .divide(BigDecimal.valueOf(investorWeeksTarget - noviceWeeksTarget), 4,
                RoundingMode.HALF_UP);
      }
      case NOVICE -> {
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

  private PlayerStatus getNextStatusTarget() {
    calculateStatus();
    return switch (status) {
      case NOVICE -> PlayerStatus.INVESTOR;
      case INVESTOR -> PlayerStatus.SPECULATOR;
      case SPECULATOR -> PlayerStatus.SPECULATOR;
    };
  }

  public BigDecimal getNetWorthGrowthRatio() {
    if (startingMoney.compareTo(BigDecimal.ZERO) == 0) {
      return BigDecimal.ZERO;
    }
    return getNetWorth().divide(startingMoney, 4, RoundingMode.HALF_UP);
  }

  public BigDecimal getGrowthTargetForNextStatus() {
    return status.getGrowthTargetForNextStatus();
  }

  public int getWeeksTargetForNextStatus() {
    return status.getWeeksTargetForNextStatus();
  }

  /**
   * Gets the player's status.
   *
   * @return the player's status
   */
  public PlayerStatus getStatus() {
    return status;
  }

  public String getProfileAvatar() {
    return profileAvatar;
  }

  public String getDisplayedProfileAvatar() {
    if (!isChickAvatarEquipped()) {
      return profileAvatar;
    }
    return getChickPhaseStem(calculateChickPhase(weeksUsingChickAvatar));
  }

  public void setProfileAvatar(String profileAvatar) {
    if (profileAvatar == null || profileAvatar.isBlank()) {
      this.profileAvatar = DEFAULT_PROFILE_AVATAR;
      return;
    }
    this.profileAvatar = normalizeAvatarSelection(profileAvatar);
  }

  /**
   * Call this at the end of each week to update chick avatar progression.
   */
  public void updateChickAvatarProgression() {
    if (!isChickAvatarEquipped()) {
      return;
    }
    weeksUsingChickAvatar = Math.min(CHICK_MAX_WEEKS, weeksUsingChickAvatar + 1);
  }

  public int getWeeksUsingChickAvatar() {
    return weeksUsingChickAvatar;
  }

  public int getChickPhaseUnlocked() {
    return calculateChickPhase(weeksUsingChickAvatar);
  }

  public void setWeeksUsingChickAvatar(int weeks) {
    this.weeksUsingChickAvatar = Math.max(0, Math.min(weeks, CHICK_MAX_WEEKS));
  }

  public boolean isChickAvatarEquipped() {
    return isChickAvatar(profileAvatar);
  }

  private String normalizeAvatarSelection(String avatar) {
    if (isChickAvatar(avatar)) {
      return CHICK_AVATAR_SELECTION;
    }
    return avatar;
  }

  private boolean isChickAvatar(String avatar) {
    if (avatar == null) {
      return false;
    }
    return avatar.equals("egg") || avatar.equals("cracking-egg") ||
        avatar.equals("hatching-chick") || avatar.equals("chick");
  }

  private int calculateChickPhase(int weeks) {
    if (weeks >= CHICK_MAX_WEEKS) {
      return 3;
    }
    if (weeks >= CHICK_PHASE_WEEKS * 2) {
      return 2;
    }
    if (weeks >= CHICK_PHASE_WEEKS) {
      return 1;
    }
    return 0;
  }

  private String getChickPhaseStem(int phase) {
    return switch (Math.max(0, Math.min(phase, CHICK_MAX_PHASE))) {
      case 0 -> "egg";
      case 1 -> "cracking-egg";
      case 2 -> "hatching-chick";
      default -> "chick";
    };
  }

  /**
   * Checks if the player owns any shares of a stock with the given symbol
   *
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
   *
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
   *
   * @param status the status to set
   */
  public void setStatus(PlayerStatus status) {
    if (status == null) {
      throw new IllegalArgumentException("Status cannot be null");
    }
    this.status = status;
  }
}