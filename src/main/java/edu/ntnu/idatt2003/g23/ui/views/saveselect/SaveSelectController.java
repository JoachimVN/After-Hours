package edu.ntnu.idatt2003.g23.ui.views.saveselect;

import edu.ntnu.idatt2003.g23.io.GameSaveLoader;
import edu.ntnu.idatt2003.g23.io.GameSaveLoader.SaveMeta;
import edu.ntnu.idatt2003.g23.io.GameUiState;
import edu.ntnu.idatt2003.g23.model.Exchange;
import edu.ntnu.idatt2003.g23.model.Player;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/**
 * Controller for the save-selection screen.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Load the list of saves from disk</li>
 *   <li>Trigger loading a chosen save (reconstructs Player + Exchange)</li>
 *   <li>Delegate deleting / renaming to {@link GameSaveLoader}</li>
 * </ul>
 */
public final class SaveSelectController {

  /**
   * Called when the user wants to start a new game (→ SetupView).
   */
  private final Runnable onNewGame;
  /**
   * Called when the user wants to go back (→ LandingPageView).
   */
  private final Runnable onBack;
  /**
   * Called when a save is successfully loaded.
   */
  private final Consumer<Object[]> onLoad; // [Player, Exchange, Path|null]
  /**
   * Called when the user wants to edit a save's market CSV in the CSV editor.
   */
  private final Consumer<SaveMeta> onEdit;
  /**
   * In-memory game session from the previous play (not persisted to disk).
   * {@code null} when there is no active session to resume.
   */
  private final Player sessionPlayer;
  private final Exchange sessionExchange;
  private final java.nio.file.Path sessionSavePath;
  private final GameUiState sessionUiState;
  private final boolean sessionFlagged;
  private final boolean devModeEnabled;

  public SaveSelectController(Runnable onNewGame, Runnable onBack,
                              Consumer<Object[]> onLoad,
                              Consumer<SaveMeta> onEdit,
                              Player sessionPlayer, Exchange sessionExchange,
                              java.nio.file.Path sessionSavePath,
                              GameUiState sessionUiState,
                              boolean sessionFlagged,
                              boolean devModeEnabled) {
    this.onNewGame = onNewGame;
    this.onBack = onBack;
    this.onLoad = onLoad;
    this.onEdit = onEdit;
    this.sessionPlayer = sessionPlayer;
    this.sessionExchange = sessionExchange;
    this.sessionSavePath = sessionSavePath;
    this.sessionUiState = sessionUiState;
    this.devModeEnabled = devModeEnabled;
    this.sessionFlagged = sessionFlagged;
  }

  // ── Actions ───────────────────────────────────────────────────────────────

  public void handleBack() {
    onBack.run();
  }

  public void handleNewGame() {
    onNewGame.run();
  }

  public void handleEditSave(SaveMeta meta) {
    if (devModeEnabled && onEdit != null && meta != null) {
      onEdit.accept(meta);
    }
  }

  public boolean isDevModeEnabled() {
    return devModeEnabled;
  }

  /**
   * Whether there is an in-memory game session to resume.
   */
  public boolean hasSession() {
    return sessionPlayer != null;
  }

  public String getSessionPlayerName() {
    return sessionPlayer != null ? sessionPlayer.getName() : "Player";
  }

  public String getSessionPlayerAvatar() {
    return sessionPlayer != null ? sessionPlayer.getProfileAvatar() : "\uD83E\uDDD1";
  }

  /**
   * Fires {@code onLoad} with the in-memory session (no disk I/O).
   */
  public void resumeSession() {
    onLoad.accept(new Object[] {sessionPlayer, sessionExchange, sessionSavePath, sessionUiState,
      sessionFlagged});
  }

  /**
   * Loads the save at {@code saveDir} and fires {@code onLoad} with
   * {@code [Player, Exchange]}.
   *
   * @return error message, or {@code null} on success
   */
  public String loadSave(Path saveDir) {
    try {
      Object[] result = GameSaveLoader.load(saveDir);
      // [0]=Player, [1]=Exchange, [2]=GameUiState (may be null)
      // Reorder to [0]=Player, [1]=Exchange, [2]=Path, [3]=UiState, [4]=flagged
      Object[] withPath = new Object[] {result[0], result[1], saveDir, result[2], result[3]};
      onLoad.accept(withPath);
      return null;
    } catch (IOException | IllegalStateException e) {
      return "Could not load save: " + e.getMessage();
    }
  }

  /**
   * Returns all saves from disk sorted newest-first, or an empty list on
   * failure.
   */
  public List<SaveMeta> loadSaveList() {
    try {
      return GameSaveLoader.listSaves();
    } catch (IOException e) {
      return List.of();
    }
  }

  /**
   * Deletes the given save and returns the updated list.
   *
   * @return error message, or {@code null} on success
   */
  public String deleteSave(Path saveDir) {
    try {
      GameSaveLoader.deleteSave(saveDir);
      return null;
    } catch (IOException e) {
      return "Could not delete save: " + e.getMessage();
    }
  }

  /**
   * Renames the save folder. The {@code newFolderName} must be a valid
   * filesystem name (sanitise before calling).
   *
   * @return the new path on success, or {@code null} on failure
   */
  public Path renameSave(Path saveDir, String newFolderName, String newDisplayName) {
    try {
      return GameSaveLoader.renameSave(saveDir, newFolderName, newDisplayName);
    } catch (IOException e) {
      return null;
    }
  }
}
