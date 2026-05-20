package edu.ntnu.idatt2003.g23.model;

import java.math.BigDecimal;

/**
 * Represents the status of a player in the stock market game.
 */
public enum PlayerStatus {
  NOVICE(10, BigDecimal.valueOf(1.2), new BigDecimal("0.30")),
  INVESTOR(20, BigDecimal.valueOf(2.0), new BigDecimal("0.25")),
  SPECULATOR(20, BigDecimal.valueOf(2.0), new BigDecimal("0.20"));

  private final int weeksTargetForNextStatus;
  private final BigDecimal growthTargetForNextStatus;
  private final BigDecimal taxRate;

  PlayerStatus(int weeksTargetForNextStatus, BigDecimal growthTargetForNextStatus,
               BigDecimal taxRate) {
    this.weeksTargetForNextStatus = weeksTargetForNextStatus;
    this.growthTargetForNextStatus = growthTargetForNextStatus;
    this.taxRate = taxRate;
  }

  public int getWeeksTargetForNextStatus() {
    return weeksTargetForNextStatus;
  }

  public BigDecimal getGrowthTargetForNextStatus() {
    return growthTargetForNextStatus;
  }

  public BigDecimal getTaxRate() {
    return taxRate;
  }
}