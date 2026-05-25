package edu.ntnu.idatt2003.g23.session;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import edu.ntnu.idatt2003.g23.io.GameUiState;
import edu.ntnu.idatt2003.g23.model.Exchange;
import edu.ntnu.idatt2003.g23.model.Player;

/**
 * Holds and updates the currently active game session state.
 */
public final class GameSessionService {

  private static final DateTimeFormatter AUTOSAVE_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

  private Player player;
  private Exchange exchange;
  private Path savePath;
  private GameUiState uiState;
  private boolean flagged;
  private String profileAvatar = "bust-in-silhouette";
  private String autosaveId;

  public void clearForNewGame() {
    player = null;
    exchange = null;
    savePath = null;
    uiState = null;
    flagged = false;
    profileAvatar = "bust-in-silhouette";
    autosaveId = null;
  }

  public void startSession(
      Player previousPlayer,
      Exchange previousExchange,
      String previousAutosaveId,
      Player newPlayer,
      Exchange newExchange,
      Path newSavePath) {

    player = newPlayer;
    exchange = newExchange;
    savePath = newSavePath;
    profileAvatar = newPlayer.getProfileAvatar();

    if (newSavePath != null) {
      autosaveId = normalizeAutosaveSlotId(newSavePath.getFileName().toString());
      return;
    }

    boolean resumingSameInMemorySession = previousAutosaveId != null
        && previousPlayer == newPlayer
        && previousExchange == newExchange;

    if (resumingSameInMemorySession) {
      autosaveId = previousAutosaveId;
      return;
    }

    flagged = false;
    String safeName = newPlayer.getName().replaceAll("[^A-Za-z0-9_\\-]", "_");
    autosaveId = safeName + "_" + LocalDateTime.now().format(AUTOSAVE_TS);
  }

  public static String normalizeAutosaveSlotId(String rawId) {
    String safe = rawId == null ? "" : rawId.replaceAll("[^A-Za-z0-9_\\-]", "_");
    while (safe.startsWith("autosave_")) {
      safe = safe.substring("autosave_".length());
    }
    if (safe.isBlank()) {
      return "slot";
    }
    return safe;
  }

  public Player getPlayer() {
    return player;
  }

  public Exchange getExchange() {
    return exchange;
  }

  public Path getSavePath() {
    return savePath;
  }

  public void setSavePath(Path savePath) {
    this.savePath = savePath;
  }

  public GameUiState getUiState() {
    return uiState;
  }

  public void setUiState(GameUiState uiState) {
    this.uiState = uiState;
  }

  public boolean isFlagged() {
    return flagged;
  }

  public void setFlagged(boolean flagged) {
    this.flagged = flagged;
  }

  public String getProfileAvatar() {
    return profileAvatar;
  }

  public void setProfileAvatar(String profileAvatar) {
    if (profileAvatar != null && !profileAvatar.isBlank()) {
      this.profileAvatar = profileAvatar;
    }
  }

  public String getAutosaveId() {
    return autosaveId;
  }

  public void setAutosaveId(String autosaveId) {
    this.autosaveId = autosaveId;
  }
}
