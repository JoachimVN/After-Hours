package edu.ntnu.idatt2003.g23.ui.views.setup;

import java.util.function.BiConsumer;

import edu.ntnu.idatt2003.g23.AppConfig;
import edu.ntnu.idatt2003.g23.model.MarketOption;
import edu.ntnu.idatt2003.g23.util.NumberParser;

/**
 * Controller for the new-game setup screen.
 * <p>
 * Mirrors the GameController pattern: holds callbacks and mutable form state;
 * the view is responsible solely for layout and delegating user actions here.
 * <p>
 * Market data lives in {@link AppConfig#BUILT_IN_MARKETS} (model layer).
 */
public final class SetupController {

  private final Runnable onBack;
  /**
   * Called when the user starts with a built-in market: (name, cash, csvResourcePath).
   */
  private final MarketStartHandler onStartDefault;
  /**
   * Called when the user starts with an imported CSV: (name, cash).
   */
  private final BiConsumer<String, Double> onStartCsv;

  // Mutable form state
  private int selectedMarketIndex = 0;
  private boolean useDefaultStocks = true;

  public SetupController(Runnable onBack,
                         MarketStartHandler onStartDefault,
                         BiConsumer<String, Double> onStartCsv) {
    this.onBack = onBack;
    this.onStartDefault = onStartDefault;
    this.onStartCsv = onStartCsv;
  }

  // ── View → Controller ─────────────────────────────────────────────────────

  public void handleBack() {
    onBack.run();
  }

  public void selectMarket(int index) {
    this.selectedMarketIndex = index;
  }

  public void setUseDefaultStocks(boolean useDefault) {
    this.useDefaultStocks = useDefault;
  }

  public void handleStart(String nameText, String cashText) {
    // AI-ASSISTED: Drafted/refined with AI support and validated by the team.
    String name = (nameText == null || nameText.isBlank()) ? "Player" : nameText.trim();
    double cash = parseCash(cashText);
    if (cash < 0) {
      return;
    }
    if (useDefaultStocks) {
      MarketOption market = AppConfig.BUILT_IN_MARKETS.get(selectedMarketIndex);
      onStartDefault.start(name, cash, market.csvResource());
    } else {
      onStartCsv.accept(name, cash);
    }
  }

  public boolean isCashReady(boolean presetSelected, String cashText) {
    return presetSelected || parseCash(cashText) >= 0;
  }

  // ── Accessors ─────────────────────────────────────────────────────────────

  // ── Internal ──────────────────────────────────────────────────────────────

  static final double MAX_CASH = 1_000_000_000.0; // 1 billion

  /**
   * Returns a human-readable error message for an invalid custom-cash string,
   * or {@code null} when the input is valid.
   * Returns {@code null} for blank/null input (treated as "not yet entered").
   */
  static String cashValidationMessage(String text) {
    // AI-ASSISTED: Drafted/refined with AI support and validated by the team.
    if (text == null || text.isBlank()) {
      return null;
    }
    String trimmed = text.trim();
    double val;
    try {
      val = NumberParser.parse(trimmed).doubleValue();
    } catch (RuntimeException _) {
      return "Not a valid number. Try: 2000, 8k, 12.5K";
    }
    if (val < 0) {
      return "Amount must be positive";
    }
    if (val >= MAX_CASH) {
      return "Amount too large (max: 1 billion)";
    }
    return null;
  }

  static double parseCash(String text) {
    try {
      double val = NumberParser.parse(text).doubleValue();
      if (val < 0 || val >= MAX_CASH) {
        return -1;
      }
      return val;
    } catch (RuntimeException _) {
      return -1;
    }
  }
}

