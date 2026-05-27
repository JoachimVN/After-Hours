package edu.ntnu.idatt2003.g23.ui.views.settings;

import java.nio.file.Path;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

import edu.ntnu.idatt2003.g23.io.GameSaveLoader.SaveMeta;
import javafx.stage.Stage;

/**
 * Holds all state and callbacks needed to build the Settings view.
 * Created by {@code App.buildSettingsView()} and consumed by
 * {@link SettingsView#build(SettingsController)}.
 *
 * <p>Fields are intentionally public and mutable so that {@code App} can
 * populate them in a single, readable block without a verbose builder API.
 *
 * <p>Fields are {@code null} when not applicable (e.g. in-game-only fields are
 * {@code null} in the home/setup context).
 */
public final class SettingsController {

  // AI-ASSISTED: Drafted/refined with AI support and validated by the team.

  // ── Navigation ───────────────────────────────────────────────────────────

  public final Runnable onBack;
  public final Stage stage;

  // ── Music ────────────────────────────────────────────────────────────────

  public double musicVolume;
  public boolean musicMuted;
  public DoubleConsumer onMusicVolumeChange;
  public Consumer<Boolean> onMusicMutedChange;

  // ── SFX ──────────────────────────────────────────────────────────────────

  public double sfxVolume;
  public boolean sfxMuted;
  public DoubleConsumer onSfxVolumeChange;
  public Consumer<Boolean> onSfxMutedChange;

  // ── Visual ───────────────────────────────────────────────────────────────

  public boolean animationsEnabled;
  public Consumer<Boolean> onAnimationsChange;
  public boolean fullscreenEnabled;
  public Consumer<Boolean> onFullscreenChange;

  // ── Gameplay / performance ────────────────────────────────────────────────

  public boolean devModeEnabled;
  public Consumer<Boolean> onDevModeChange;
  public boolean autosaveEnabled;
  public Consumer<Boolean> onAutosaveChange;
  public boolean autosaveToast;
  public Consumer<Boolean> onAutosaveToastChange;
  public boolean showTutorial;
  public Consumer<Boolean> onShowTutorialChange;
  public boolean performanceModeEnabled;
  public Consumer<Boolean> onPerformanceModeChange;
  public int maxHistoryWeeks;
  public Consumer<Integer> onMaxHistoryWeeksChange;

  // ── In-game context (null in home/setup context) ──────────────────────────

  /** Non-null only when the player is in an active game session. */
  public Path currentSavePath;
  /** Non-null only in-game; triggers the "Save Game" button in SettingsView. */
  public Runnable onSave;
  /** Resets all settings to defaults and re-opens the settings page. */
  public Runnable onResetAll;
  /** Allows the user to pick a preset window resolution. */
  public Consumer<int[]> onResolutionChange;
  /** Maximizes the window. */
  public Runnable onMaximize;
  /** Exports selected save as JSON + CSV. Returns exported file name, null if canceled. */
  public BiFunction<SaveMeta, Boolean, String> onExportJsonCsv;
  /** Exports selected save as CSV only. Returns exported file name, null if canceled. */
  public BiFunction<SaveMeta, Boolean, String> onExportCsvOnly;
  /** Current player name shown in the name-change field. In-game only. */
  public String currentPlayerName;
  /** Notified when the player changes their display name. In-game only. */
  public Consumer<String> onNameChanged;

  // ── CSV Data Editing ──────────────────────────────────────────────────────

  /** Opens the CSV tools page. Available in all contexts. */
  public Runnable onOpenCsvTools;
  /** Opens the current in-game market directly in the CSV editor. In-game only. */
  public Runnable onEditCurrentMarketData;

  // ── Constructor ───────────────────────────────────────────────────────────

  public SettingsController(Runnable onBack, Stage stage) {
    this.onBack = onBack;
    this.stage = stage;
  }
}
