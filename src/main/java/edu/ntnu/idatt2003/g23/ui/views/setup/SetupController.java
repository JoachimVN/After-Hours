package edu.ntnu.idatt2003.g23.ui.views.setup;

import java.util.function.BiConsumer;

import edu.ntnu.idatt2003.g23.AppConfig;
import edu.ntnu.idatt2003.g23.model.MarketOption;
import edu.ntnu.idatt2003.g23.util.NumberParser;

/**
 * Controller for the new-game setup screen.
 *
 * Mirrors the GameController pattern: holds callbacks and mutable form state;
 * the view is responsible solely for layout and delegating user actions here.
 *
 * Market data lives in {@link AppConfig#BUILT_IN_MARKETS} (model layer).
 */
public final class SetupController {

    private static final double[] PRESET_VALUES = {1_000, 5_000, 10_000, 50_000, 100_000};
    private static final String[] PRESET_LABELS = {"$1K", "$5K", "$10K", "$50K", "$100K"};

    private final Runnable onBack;
    /** Called when the user starts with a built-in market: (name, cash, csvResourcePath). */
    private final MarketStartHandler onStartDefault;
    /** Called when the user starts with an imported CSV: (name, cash). */
    private final BiConsumer<String, Double> onStartCsv;

    // Mutable form state
    private int selectedMarketIndex = 0;
    private boolean useDefaultStocks = true;

    public SetupController(Runnable onBack,
                           MarketStartHandler onStartDefault,
                           BiConsumer<String, Double> onStartCsv) {
        this.onBack         = onBack;
        this.onStartDefault = onStartDefault;
        this.onStartCsv     = onStartCsv;
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
        String name = (nameText == null || nameText.isBlank()) ? "Player" : nameText.trim();
        double cash = parseCash(cashText);
        if (useDefaultStocks) {
            MarketOption market = AppConfig.BUILT_IN_MARKETS.get(selectedMarketIndex);
            onStartDefault.start(name, cash, market.csvResource());
        } else {
            onStartCsv.accept(name, cash);
        }
    }

    public boolean isCashReady(boolean presetSelected, String cashText) {
        return presetSelected || (!cashText.isBlank() && parseCash(cashText) > 0);
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public double[] getPresetValues() { return PRESET_VALUES; }
    public String[] getPresetLabels() { return PRESET_LABELS; }
    public int getDefaultMarketIndex() { return 0; }

    // ── Internal ──────────────────────────────────────────────────────────────

    static double parseCash(String text) {
        try {
            double val = NumberParser.parse(text).doubleValue();
            return val > 0 ? val : 10_000;
        } catch (NumberFormatException e) {
            return 10_000;
        }
    }
}

