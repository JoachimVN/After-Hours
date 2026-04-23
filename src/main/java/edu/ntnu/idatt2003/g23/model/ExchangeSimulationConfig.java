package edu.ntnu.idatt2003.g23.model;

/**
 * Centralized tuning values for exchange price simulation.
 */
public final class ExchangeSimulationConfig {
  public static final double MEAN_REVERSION_STRENGTH = 0.006;
  public static final int MAX_SPIKE_TARGETS_PER_EVENT = 3;
  public static final double ADDITIONAL_SPIKE_TARGET_CHANCE = 0.12;

  private ExchangeSimulationConfig() {
  }
}
