package edu.ntnu.idatt2003.g23.model;

import java.math.BigDecimal;

/**
 * Represents the status of a player in the stock market game.
 */
public enum PlayerStatus {
  NOVICE(10, BigDecimal.valueOf(1.2)),
  INVESTOR(20, BigDecimal.valueOf(2.0)),
  SPECULATOR(20, BigDecimal.valueOf(2.0));

  private final int weeksTargetForNextStatus;
  private final BigDecimal growthTargetForNextStatus;

  PlayerStatus(int weeksTargetForNextStatus, BigDecimal growthTargetForNextStatus) {
    this.weeksTargetForNextStatus = weeksTargetForNextStatus;
    this.growthTargetForNextStatus = growthTargetForNextStatus;
  }

  public int getWeeksTargetForNextStatus() {
    return weeksTargetForNextStatus;
  }

  public BigDecimal getGrowthTargetForNextStatus() {
    return growthTargetForNextStatus;
  }
}