package edu.ntnu.idatt2003.g23;

import java.util.List;

import edu.ntnu.idatt2003.g23.model.MarketOption;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.util.Duration;

public final class AppConfig {

  public static final String APP_TITLE = "After Hours";
  public static final double DEFAULT_WIDTH = 1024;
  public static final double DEFAULT_HEIGHT = 768;
  public static final double MIN_WIDTH = 860;
  public static final double MIN_HEIGHT = 620;
    public static final Duration SPLASH_LOGO_FADE_DURATION = Duration.seconds(1.5);
  public static final Duration SPLASH_FADE_DURATION = Duration.seconds(0.6);
  public static final Duration SPLASH_DELAY =
      Duration.seconds(110.0 / 60.0 - (SPLASH_FADE_DURATION.toSeconds() * 0.25));
  // 110 BPM (Song BPM) / 60 (seconds per beat) - quarter of the fade duration (aligns well)
  public static final Duration SPLASH_FALLBACK_DELAY = Duration.seconds(0.3);

  /**
   * Global dev-mode flag — toggled from Settings.
   */
  public static final BooleanProperty DEV_MODE = new SimpleBooleanProperty(false);

    /**
     * Global performance-mode flag — toggled from Settings.
     */
    public static final BooleanProperty PERFORMANCE_MODE = new SimpleBooleanProperty(false);

    /**
     * Max retained historical weeks per stock when performance mode is enabled.
     */
    public static final IntegerProperty PERFORMANCE_MAX_HISTORY_WEEKS =
            new SimpleIntegerProperty(500);

  /**
   * Built-in markets available on the setup screen.
   */
  public static final List<MarketOption> BUILT_IN_MARKETS = List.of(
      new MarketOption("NASDAQ-100", "data/stocks/nasdaq100.csv",
          "100 top Nasdaq tech-heavy companies."),
      new MarketOption("S\u0026P 500", "data/stocks/sp500.csv",
          "500 large U.S. companies tracking the market."),
      new MarketOption("OBX", "data/stocks/obx.csv", "25 most liquid stocks on Oslo B\u00f8rs.")
  );

  /**
   * Preset starting-cash options shown on the setup screen.
   */
  public static final double[] PRESET_CASH_VALUES = {1_000, 5_000, 10_000, 50_000, 100_000};
  public static final String[] PRESET_CASH_LABELS = {"$1K", "$5K", "$10K", "$50K", "$100K"};

  /**
   * Returns the display name of the built-in market matching the given CSV resource path,
   * or {@code "Market"} if no match is found.
   */
  public static String marketNameFor(String csvResource) {
    return BUILT_IN_MARKETS.stream()
        .filter(m -> m.csvResource().equals(csvResource))
        .map(m -> m.name())
        .findFirst()
        .orElse("Market");
  }

  private AppConfig() {
  }
}

